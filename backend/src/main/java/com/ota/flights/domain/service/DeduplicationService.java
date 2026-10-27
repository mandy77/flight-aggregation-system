package com.ota.flights.domain.service;

import com.ota.flights.domain.model.Itinerary;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Identifies identical itineraries across providers and keeps the cheapest.
 * Identity = airline + flight numbers + connections + departure times.
 */
public class DeduplicationService {

    public List<Itinerary> deduplicate(List<Itinerary> itineraries) {
        Map<String, Itinerary> bestByKey = new LinkedHashMap<>();

        for (Itinerary itinerary : itineraries) {
            String key = itinerary.identityKey();
            Itinerary existing = bestByKey.get(key);
            if (existing == null || itinerary.getBasePrice().compareTo(existing.getBasePrice()) < 0) {
                bestByKey.put(key, itinerary);
            }
        }
        return new ArrayList<>(bestByKey.values());
    }
}
