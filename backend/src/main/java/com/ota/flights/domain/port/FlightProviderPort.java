package com.ota.flights.domain.port;

import com.ota.flights.domain.model.Itinerary;

import java.util.List;

/**
 * Port for a single GDS / mock provider.
 */
public interface FlightProviderPort {

    String getName();

    /**
     * Fetch raw itineraries. May throw on simulated failure.
     */
    List<Itinerary> search(String origin, String destination, String departureDate);
}
