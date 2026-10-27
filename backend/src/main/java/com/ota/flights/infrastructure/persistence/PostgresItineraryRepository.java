package com.ota.flights.infrastructure.persistence;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ota.flights.domain.model.*;
import com.ota.flights.domain.port.ItineraryRepositoryPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class PostgresItineraryRepository implements ItineraryRepositoryPort {

    private final SpringDataItineraryRepository jpa;
    private final ObjectMapper mapper;

    public PostgresItineraryRepository(SpringDataItineraryRepository jpa) {
        this.jpa = jpa;
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    @Transactional
    public int saveMany(List<PricedItinerary> itineraries) {
        if (itineraries.isEmpty()) return 0;
        List<ItineraryEntity> entities = new ArrayList<>(itineraries.size());
        for (PricedItinerary p : itineraries) {
            entities.add(toEntity(p));
        }
        jpa.saveAll(entities);
        return entities.size();
    }

    @Override
    @Transactional
    public void clear() {
        jpa.truncate();
    }

    @Override
    @Transactional(readOnly = true)
    public SearchResult search(SearchCriteria criteria) {
        BigDecimal cursorPrice = null;
        String cursorId = null;
        if (criteria.getCursor() != null && !criteria.getCursor().isBlank()) {
            try {
                String raw = new String(Base64.getUrlDecoder().decode(criteria.getCursor()), StandardCharsets.UTF_8);
                String[] parts = raw.split("\\|", 2);
                cursorPrice = new BigDecimal(parts[0]);
                cursorId = parts[1];
            } catch (Exception ignored) {
                // bad cursor → start from beginning
            }
        }

        int fetch = criteria.getLimit() + 1;
        List<ItineraryEntity> rows = jpa.searchKeyset(
                criteria.getOrigin(),
                criteria.getDestination(),
                criteria.getMaxPrice(),
                criteria.getCarrier(),
                criteria.getMaxStops(),
                criteria.getCabinClass() != null ? criteria.getCabinClass().name().toLowerCase() : null,
                cursorPrice,
                cursorId,
                fetch
        );

        boolean hasMore = rows.size() > criteria.getLimit();
        if (hasMore) {
            rows = rows.subList(0, criteria.getLimit());
        }

        List<PricedItinerary> items = rows.stream().map(this::toDomain).toList();
        String nextCursor = null;
        if (hasMore && !items.isEmpty()) {
            PricedItinerary last = items.getLast();
            nextCursor = encodeCursor(last.getFinalPrice(), last.getId());
        }
        return new SearchResult(items, nextCursor, hasMore);
    }

    @Override
    @Transactional(readOnly = true)
    public long count(SearchCriteria criteria) {
        return jpa.countFiltered(
                criteria.getOrigin(),
                criteria.getDestination(),
                criteria.getMaxPrice(),
                criteria.getCarrier()
        );
    }

    private ItineraryEntity toEntity(PricedItinerary p) {
        Itinerary itinerary = p.getItinerary();
        ItineraryEntity e = new ItineraryEntity();
        e.setId(itinerary.getId());
        e.setIdentityKey(itinerary.identityKey());
        e.setOrigin(itinerary.getOrigin());
        e.setDestination(itinerary.getDestination());
        e.setDepartureTime(itinerary.getDepartureTime());
        e.setArrivalTime(itinerary.getArrivalTime());
        e.setCarriers(itinerary.getCarriers().toArray(String[]::new));
        e.setFlightNumbers(itinerary.getFlightNumbers().toArray(String[]::new));
        e.setStops(itinerary.getStops());
        e.setDurationMins(itinerary.getTotalDurationMinutes());
        e.setBasePrice(itinerary.getBasePrice());
        e.setFinalPrice(p.getFinalPrice());
        e.setMarkupPercent(p.getMarkupPercent());
        e.setCurrency(itinerary.getCurrency());
        e.setProvider(itinerary.getProvider());
        e.setCabinClass(itinerary.getCabinClass().name().toLowerCase());
        e.setSeatsAvailable(itinerary.getSeatsAvailable());
        try {
            List<Map<String, Object>> segs = new ArrayList<>();
            for (FlightSegment s : itinerary.getSegments()) {
                segs.add(Map.of(
                        "airline", s.getAirlineCode(),
                        "flight", s.getFlightNumber(),
                        "origin", s.getOrigin(),
                        "destination", s.getDestination(),
                        "departure", s.getDepartureTime().toString(),
                        "arrival", s.getArrivalTime().toString(),
                        "duration", s.getDurationMinutes(),
                        "aircraft", s.getAircraftType() != null ? s.getAircraftType() : ""
                ));
            }
            e.setSegmentsJson(mapper.writeValueAsString(segs));
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return e;
    }

    private PricedItinerary toDomain(ItineraryEntity e) {
        try {
            List<Map<String, Object>> segMaps = mapper.readValue(
                    e.getSegmentsJson(), new TypeReference<>() {});
            List<FlightSegment> segments = new ArrayList<>();
            for (Map<String, Object> s : segMaps) {
                segments.add(new FlightSegment(
                        (String) s.get("airline"),
                        (String) s.get("flight"),
                        (String) s.get("origin"),
                        (String) s.get("destination"),
                        LocalDateTime.parse((String) s.get("departure")),
                        LocalDateTime.parse((String) s.get("arrival")),
                        ((Number) s.get("duration")).intValue(),
                        s.get("aircraft") != null && !((String) s.get("aircraft")).isEmpty()
                                ? (String) s.get("aircraft") : null
                ));
            }
            Itinerary itin = new Itinerary(
                    e.getId(),
                    segments,
                    e.getBasePrice(),
                    e.getCurrency(),
                    e.getProvider(),
                    CabinClass.from(e.getCabinClass()),
                    e.getSeatsAvailable()
            );
            return new PricedItinerary(itin, e.getFinalPrice(), e.getMarkupPercent());
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static String encodeCursor(BigDecimal price, String id) {
        String raw = price.toPlainString() + "|" + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }
}
