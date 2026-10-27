package com.ota.flights.interfaces.rest;

import com.ota.flights.application.IngestFlightsUseCase;
import com.ota.flights.application.SearchFlightsUseCase;
import com.ota.flights.domain.model.*;
import com.ota.flights.interfaces.rest.dto.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@CrossOrigin(origins = "*")
public class FlightController {

    private final IngestFlightsUseCase ingestUseCase;
    private final SearchFlightsUseCase searchUseCase;

    public FlightController(IngestFlightsUseCase ingestUseCase, SearchFlightsUseCase searchUseCase) {
        this.ingestUseCase = ingestUseCase;
        this.searchUseCase = searchUseCase;
    }

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "Flight Aggregation System (Java / Spring Boot)",
                "docs", "/swagger-ui.html",
                "endpoints", List.of("/health", "/ingest", "/search")
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok", "database", "ok", "redis", "ok");
    }

    @PostMapping("/ingest")
    public IngestResponse ingest(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) String departureDate) {

        Map<String, Object> result = ingestUseCase.execute(origin, destination, departureDate);

        @SuppressWarnings("unchecked")
        List<String> ok = (List<String>) result.get("providersOk");
        @SuppressWarnings("unchecked")
        List<Map<String, String>> failed = (List<Map<String, String>>) result.get("providersFailed");

        return new IngestResponse(
                (int) result.get("ingested"),
                (int) result.get("rawCollected"),
                (int) result.get("afterDedupAndPricing"),
                ok,
                failed
        );
    }

    @GetMapping("/search")
    public SearchResponse search(
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String destination,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String carrier,
            @RequestParam(required = false) Integer maxStops,
            @RequestParam(required = false) String cabinClass,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String cursor) {

        CabinClass cabin = null;
        if (cabinClass != null && !cabinClass.isBlank()) {
            cabin = CabinClass.from(cabinClass);
        }

        SearchCriteria criteria = new SearchCriteria(
                origin, destination, maxPrice, carrier, maxStops, cabin, limit, cursor);

        SearchResult result = searchUseCase.execute(criteria);

        List<ItineraryDto> items = result.getItems().stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new SearchResponse(items, result.getNextCursor(), result.isHasMore(), items.size());
    }

    private ItineraryDto toDto(PricedItinerary p) {
        Itinerary itinerary = p.getItinerary();
        List<SegmentDto> segs = itinerary.getSegments().stream()
                .map(s -> new SegmentDto(
                        s.getAirlineCode(),
                        s.getFlightNumber(),
                        s.getOrigin(),
                        s.getDestination(),
                        s.getDepartureTime().toString(),
                        s.getArrivalTime().toString(),
                        s.getDurationMinutes(),
                        s.getAircraftType()
                ))
                .toList();

        return new ItineraryDto(
                itinerary.getId(),
                itinerary.getOrigin(),
                itinerary.getDestination(),
                itinerary.getDepartureTime().toString(),
                itinerary.getArrivalTime().toString(),
                itinerary.getStops(),
                itinerary.getTotalDurationMinutes(),
                itinerary.getCarriers(),
                itinerary.getFlightNumbers(),
                itinerary.getBasePrice().doubleValue(),
                p.getFinalPrice().doubleValue(),
                itinerary.getCurrency(),
                itinerary.getProvider(),
                itinerary.getCabinClass().name().toLowerCase(),
                itinerary.getSeatsAvailable(),
                segs
        );
    }
}
