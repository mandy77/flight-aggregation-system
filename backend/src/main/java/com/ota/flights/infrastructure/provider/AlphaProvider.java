package com.ota.flights.infrastructure.provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ota.flights.domain.model.CabinClass;
import com.ota.flights.domain.model.FlightSegment;
import com.ota.flights.domain.model.Itinerary;
import com.ota.flights.domain.port.FlightProviderPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Mock-Alpha: returns raw JSON lists.
 * Latency 100–800 ms, ~7% failure rate.
 */
public class AlphaProvider implements FlightProviderPort {

    private final int volume;
    private final ObjectMapper mapper;

    public AlphaProvider(int volume) {
        this.volume = volume;
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Override
    public String getName() {
        return "Mock-Alpha";
    }

    @Override
    public List<Itinerary> search(String origin, String destination, String departureDate) {
        ProviderSupport.simulateLatency(100, 800);
        ProviderSupport.maybeFail(0.07);

        List<Itinerary> raw = ProviderSupport.generateItineraries(
                volume, origin, destination, departureDate, getName());

        // Round-trip through JSON to mimic real deserialization
        try {
            String json = mapper.writeValueAsString(toJsonList(raw));
            List<Map<String, Object>> parsed = mapper.readValue(json, new TypeReference<>() {});
            return fromJsonList(parsed);
        } catch (Exception e) {
            throw new RuntimeException("Alpha JSON parse error", e);
        }
    }

    private List<Map<String, Object>> toJsonList(List<Itinerary> itineraries) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Itinerary itinerary : itineraries) {
            List<Map<String, Object>> segs = new ArrayList<>();
            for (FlightSegment s : itinerary.getSegments()) {
                segs.add(Map.of(
                        "airline", s.getAirlineCode(),
                        "flight", s.getFlightNumber(),
                        "from", s.getOrigin(),
                        "to", s.getDestination(),
                        "dep", s.getDepartureTime().toString(),
                        "arr", s.getArrivalTime().toString(),
                        "mins", s.getDurationMinutes(),
                        "aircraft", s.getAircraftType() != null ? s.getAircraftType() : ""
                ));
            }
            list.add(Map.of(
                    "id", itinerary.getId(),
                    "provider", itinerary.getProvider(),
                    "price", itinerary.getBasePrice().toPlainString(),
                    "currency", itinerary.getCurrency(),
                    "cabin", itinerary.getCabinClass().name().toLowerCase(),
                    "seats", itinerary.getSeatsAvailable(),
                    "segments", segs
            ));
        }
        return list;
    }

    @SuppressWarnings("unchecked")
    private List<Itinerary> fromJsonList(List<Map<String, Object>> data) {
        List<Itinerary> result = new ArrayList<>();
        for (Map<String, Object> item : data) {
            List<Map<String, Object>> segMaps = (List<Map<String, Object>>) item.get("segments");
            List<FlightSegment> segments = new ArrayList<>();
            for (Map<String, Object> s : segMaps) {
                segments.add(new FlightSegment(
                        (String) s.get("airline"),
                        (String) s.get("flight"),
                        (String) s.get("from"),
                        (String) s.get("to"),
                        LocalDateTime.parse((String) s.get("dep")),
                        LocalDateTime.parse((String) s.get("arr")),
                        ((Number) s.get("mins")).intValue(),
                        (String) s.get("aircraft")
                ));
            }
            result.add(new Itinerary(
                    (String) item.get("id"),
                    segments,
                    new BigDecimal((String) item.get("price")),
                    (String) item.get("currency"),
                    (String) item.get("provider"),
                    CabinClass.from((String) item.get("cabin")),
                    ((Number) item.get("seats")).intValue()
            ));
        }
        return result;
    }
}
