package com.ota.flights.infrastructure.provider;

import com.ota.flights.domain.model.CabinClass;
import com.ota.flights.domain.model.FlightSegment;
import com.ota.flights.domain.model.Itinerary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

final class ProviderSupport {

    static final String[] AIRLINES = {
            "AA", "UA", "DL", "BA", "LH", "AF", "EK", "QR", "SQ", "CX"
    };
    static final String[] AIRPORTS = {
            "JFK", "LAX", "ORD", "ATL", "DFW", "SFO", "MIA", "BOS", "SEA", "DEN",
            "LHR", "CDG", "FRA", "AMS", "MAD", "FCO", "DXB", "SIN", "HKG", "NRT",
            "SYD", "MEL", "YYZ", "YVR", "GRU", "EZE", "MEX", "CUN", "BOM", "DEL"
    };
    static final String[] AIRCRAFT = {"B737", "A320", "B787", "A350", "B777"};

    private ProviderSupport() {}

    static void simulateLatency(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs + 1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    static void maybeFail(double failureRate) {
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new RuntimeException("Simulated 500 Internal Server Error from provider");
        }
    }

    static List<Itinerary>
    generateItineraries(
            int count,
            String origin,
            String destination,
            String departureDate,
            String providerName) {

        Random rnd = ThreadLocalRandom.current();
        LocalDate baseDate = departureDate != null
                ? LocalDate.parse(departureDate)
                : LocalDate.now().plusDays(7);
        LocalDateTime base = LocalDateTime.of(baseDate, LocalTime.of(6, 0));

        List<Itinerary> results = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            String o = origin != null ? origin : AIRPORTS[rnd.nextInt(AIRPORTS.length)];
            String d;
            if (destination != null) {
                d = destination;
            } else {
                do {
                    d = AIRPORTS[rnd.nextInt(AIRPORTS.length)];
                } while (d.equals(o));
            }

            int stops = weightedStops(rnd);
            LocalDateTime dep = base.plusHours(rnd.nextInt(19)).plusMinutes(rnd.nextInt(60));
            List<FlightSegment> segments = generateSegments(o, d, dep, stops, rnd);

            BigDecimal price = BigDecimal.valueOf(80 + rnd.nextDouble() * 1720 * (1 + stops * 0.3))
                    .setScale(2, RoundingMode.HALF_UP);

            results.add(new Itinerary(
                    UUID.randomUUID().toString(),
                    segments,
                    price,
                    "USD",
                    providerName,
                    CabinClass.values()[rnd.nextInt(CabinClass.values().length)],
                    1 + rnd.nextInt(9)
            ));
        }
        return results;
    }

    private static int weightedStops(Random rnd) {
        double r = rnd.nextDouble();
        if (r < 0.55) return 0;
        if (r < 0.90) return 1;
        return 2;
    }

    private static List<FlightSegment> generateSegments(
            String origin, String destination, LocalDateTime departure, int stops, Random rnd) {

        List<String> points = new ArrayList<>();
        points.add(origin);
        if (stops > 0) {
            List<String> available = new ArrayList<>();
            for (String a : AIRPORTS) {
                if (!a.equals(origin) && !a.equals(destination)) available.add(a);
            }
            for (int i = 0; i < Math.min(stops, available.size()); i++) {
                int idx = rnd.nextInt(available.size());
                points.add(available.remove(idx));
            }
        }
        points.add(destination);

        List<FlightSegment> segments = new ArrayList<>();
        LocalDateTime currentDep = departure;

        for (int i = 0; i < points.size() - 1; i++) {
            String o = points.get(i);
            String d = points.get(i + 1);
            String airline = AIRLINES[rnd.nextInt(AIRLINES.length)];
            String flightNum = String.valueOf(100 + rnd.nextInt(9900));
            int duration = 60 + rnd.nextInt(541);
            LocalDateTime arrival = currentDep.plusMinutes(duration);

            segments.add(new FlightSegment(
                    airline, flightNum, o, d, currentDep, arrival, duration,
                    AIRCRAFT[rnd.nextInt(AIRCRAFT.length)]
            ));

            if (i < points.size() - 2) {
                int layover = 50 + rnd.nextInt(131);
                currentDep = arrival.plusMinutes(layover);
            }
        }
        return segments;
    }
}
