package com.ota.flights.infrastructure.provider;

import com.ota.flights.domain.model.CabinClass;
import com.ota.flights.domain.model.FlightSegment;
import com.ota.flights.domain.model.Itinerary;
import com.ota.flights.domain.port.FlightProviderPort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock-Gamma: comma-separated string streams (legacy flat format).
 * Latency 200–5000 ms, ~9% failure rate.
 */
public class GammaProvider implements FlightProviderPort {

    private final int volume;

    public GammaProvider(int volume) {
        this.volume = volume;
    }

    @Override
    public String getName() {
        return "Mock-Gamma";
    }

    @Override
    public List<Itinerary> search(String origin, String destination, String departureDate) {
        ProviderSupport.simulateLatency(200, 5000);
        ProviderSupport.maybeFail(0.09);

        List<Itinerary> raw = ProviderSupport.generateItineraries(
                volume, origin, destination, departureDate, getName());
        String csv = toCsv(raw);
        return fromCsv(csv);
    }

    private String toCsv(List<Itinerary> itineraries) {
        StringBuilder sb = new StringBuilder();
        sb.append("itin_id,provider,price,currency,cabin,seats,seg_idx,airline,flight,origin,dest,dep,arr,duration,aircraft\n");
        for (Itinerary itinerary : itineraries) {
            int idx = 0;
            for (FlightSegment s : itinerary.getSegments()) {
                sb.append(itinerary.getId()).append(',')
                        .append(itinerary.getProvider()).append(',')
                        .append(itinerary.getBasePrice().toPlainString()).append(',')
                        .append(itinerary.getCurrency()).append(',')
                        .append(itinerary.getCabinClass().name().toLowerCase()).append(',')
                        .append(itinerary.getSeatsAvailable()).append(',')
                        .append(idx++).append(',')
                        .append(s.getAirlineCode()).append(',')
                        .append(s.getFlightNumber()).append(',')
                        .append(s.getOrigin()).append(',')
                        .append(s.getDestination()).append(',')
                        .append(s.getDepartureTime()).append(',')
                        .append(s.getArrivalTime()).append(',')
                        .append(s.getDurationMinutes()).append(',')
                        .append(s.getAircraftType() != null ? s.getAircraftType() : "")
                        .append('\n');
            }
        }
        return sb.toString();
    }

    private List<Itinerary> fromCsv(String csv) {
        String[] lines = csv.split("\n");
        Map<String, List<String[]>> groups = new LinkedHashMap<>();
        Map<String, String[]> meta = new LinkedHashMap<>();

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            String[] cols = line.split(",", -1);
            String itinId = cols[0];
            groups.computeIfAbsent(itinId, k -> new ArrayList<>()).add(cols);
            meta.putIfAbsent(itinId, cols);
        }

        List<Itinerary> result = new ArrayList<>();
        for (Map.Entry<String, List<String[]>> e : groups.entrySet()) {
            String[] m = meta.get(e.getKey());
            List<String[]> rows = e.getValue();
            rows.sort((a, b) -> Integer.compare(Integer.parseInt(a[6]), Integer.parseInt(b[6])));

            List<FlightSegment> segments = new ArrayList<>();
            for (String[] r : rows) {
                segments.add(new FlightSegment(
                        r[7], r[8], r[9], r[10],
                        LocalDateTime.parse(r[11]),
                        LocalDateTime.parse(r[12]),
                        Integer.parseInt(r[13]),
                        r[14].isEmpty() ? null : r[14]
                ));
            }
            result.add(new Itinerary(
                    e.getKey(),
                    segments,
                    new BigDecimal(m[2]),
                    m[3],
                    m[1],
                    CabinClass.from(m[4]),
                    Integer.parseInt(m[5])
            ));
        }
        return result;
    }
}
