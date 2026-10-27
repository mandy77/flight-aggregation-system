package com.ota.flights.domain;

import com.ota.flights.domain.model.*;
import com.ota.flights.domain.service.*;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DomainServicesTest {

    private FlightSegment segment(String airline, String flight, String origin, String dest,
                                  LocalDateTime dep, int durationMins) {
        return new FlightSegment(airline, flight, origin, dest,
                dep, dep.plusMinutes(durationMins), durationMins, "B737");
    }

    private Itinerary itinerary(List<FlightSegment> segs, String price, String provider) {
        return new Itinerary(null, segs, new BigDecimal(price), "USD", provider, CabinClass.ECONOMY, 5);
    }

    @Test
    void identityKeyIsStable() {
        LocalDateTime dep = LocalDateTime.of(2026, 11, 1, 8, 0);
        FlightSegment s = segment("AA", "100", "JFK", "LAX", dep, 300);
        assertTrue(s.identityKey().startsWith("AA:100:JFK:LAX:"));
    }

    @Test
    void invalidTimesRejected() {
        LocalDateTime t = LocalDateTime.of(2026, 1, 1, 12, 0);
        assertThrows(IllegalArgumentException.class, () ->
                new FlightSegment("AA", "1", "JFK", "LAX", t, t.minusHours(1), 60, null));
    }

    @Test
    void validConnection() {
        LocalDateTime dep = LocalDateTime.of(2026, 11, 1, 8, 0);
        FlightSegment s1 = segment("AA", "100", "JFK", "ORD", dep, 120);
        FlightSegment s2 = segment("UA", "200", "ORD", "LAX",
                s1.getArrivalTime().plusMinutes(60), 240);
        Itinerary itinerary = itinerary(List.of(s1, s2), "199", "Alpha");
        assertTrue(itinerary.isValidConnection(45));
    }

    @Test
    void shortConnectionInvalid() {
        LocalDateTime dep = LocalDateTime.of(2026, 11, 1, 8, 0);
        FlightSegment s1 = segment("AA", "100", "JFK", "ORD", dep, 120);
        FlightSegment s2 = segment("UA", "200", "ORD", "LAX",
                s1.getArrivalTime().plusMinutes(20), 240);
        Itinerary itinerary = itinerary(List.of(s1, s2), "199", "Alpha");
        assertFalse(itinerary.isValidConnection(45));
    }

    @Test
    void deadLegInvalid() {
        LocalDateTime dep = LocalDateTime.of(2026, 11, 1, 8, 0);
        FlightSegment s1 = segment("AA", "100", "JFK", "ORD", dep, 120);
        FlightSegment s2 = segment("UA", "200", "ATL", "LAX",
                s1.getArrivalTime().plusMinutes(90), 240);
        Itinerary itinerary = itinerary(List.of(s1, s2), "199", "Alpha");
        assertFalse(itinerary.isValidConnection(45));
    }

    @Test
    void markupApplied() {
        LocalDateTime dep = LocalDateTime.of(2026, 11, 1, 8, 0);
        Itinerary itinerary = itinerary(List.of(segment("AA", "100", "JFK", "LAX", dep, 300)), "100.00", "A");
        PricedItinerary priced = itinerary.withMarkup(new BigDecimal("0.05"));
        assertEquals(new BigDecimal("105.00"), priced.getFinalPrice());
    }

    @Test
    void dedupKeepsCheapest() {
        LocalDateTime dep = LocalDateTime.of(2026, 11, 1, 8, 0);
        FlightSegment s = segment("AA", "100", "JFK", "LAX", dep, 300);
        Itinerary a = itinerary(List.of(s), "150.00", "Alpha");
        Itinerary b = itinerary(List.of(s), "140.00", "Beta");
        Itinerary c = itinerary(List.of(s), "160.00", "Gamma");
        List<Itinerary> result = new DeduplicationService().deduplicate(List.of(a, b, c));
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("140.00"), result.getFirst().getBasePrice());
        assertEquals("Beta", result.getFirst().getProvider());
    }

    @Test
    void aggregationPipeline() {
        LocalDateTime dep = LocalDateTime.of(2026, 11, 1, 8, 0);
        FlightSegment s = segment("AA", "100", "JFK", "LAX", dep, 300);
        List<Itinerary> raw = List.of(
                itinerary(List.of(s), "200", "A"),
                itinerary(List.of(s), "180", "B"),
                // dead leg
                itinerary(List.of(
                        segment("AA", "1", "JFK", "ORD", dep, 120),
                        segment("UA", "9", "ATL", "LAX", dep.plusHours(4), 240)
                ), "50", "C")
        );
        List<PricedItinerary> priced = new AggregationService().aggregate(raw);
        assertEquals(1, priced.size());
        assertEquals(new BigDecimal("189.00"), priced.getFirst().getFinalPrice());
        assertEquals("B", priced.getFirst().getItinerary().getProvider());
    }
}
