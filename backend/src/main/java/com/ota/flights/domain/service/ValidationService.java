package com.ota.flights.domain.service;

import com.ota.flights.domain.model.Itinerary;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Filters out invalid connections and dead legs.
 */
public class ValidationService {

    private final int minConnectionMinutes;

    public ValidationService() {
        this(45);
    }

    public ValidationService(int minConnectionMinutes) {
        this.minConnectionMinutes = minConnectionMinutes;
    }

    public List<Itinerary> filterValid(List<Itinerary> itineraries) {
        return itineraries.stream()
                .filter(itinerary -> itinerary.isValidConnection(minConnectionMinutes))
                .collect(Collectors.toList());
    }
}
