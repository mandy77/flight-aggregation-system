package com.ota.flights.domain.model;

import java.math.BigDecimal;

/**
 * Value object representing a flight search request.
 */
public final class SearchCriteria {

    private final String origin;
    private final String destination;
    private final BigDecimal maxPrice;
    private final String carrier;
    private final Integer maxStops;
    private final CabinClass cabinClass;
    private final int limit;
    private final String cursor;

    public SearchCriteria(
            String origin,
            String destination,
            BigDecimal maxPrice,
            String carrier,
            Integer maxStops,
            CabinClass cabinClass,
            int limit,
            String cursor) {

        if (origin != null && origin.length() != 3) {
            throw new IllegalArgumentException("Origin must be 3-letter IATA code");
        }
        if (destination != null && destination.length() != 3) {
            throw new IllegalArgumentException("Destination must be 3-letter IATA code");
        }
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("Limit must be between 1 and 200");
        }

        this.origin = origin != null ? origin.toUpperCase() : null;
        this.destination = destination != null ? destination.toUpperCase() : null;
        this.maxPrice = maxPrice;
        this.carrier = carrier != null ? carrier.toUpperCase() : null;
        this.maxStops = maxStops;
        this.cabinClass = cabinClass;
        this.limit = limit;
        this.cursor = cursor;
    }

    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public String getCarrier() { return carrier; }
    public Integer getMaxStops() { return maxStops; }
    public CabinClass getCabinClass() { return cabinClass; }
    public int getLimit() { return limit; }
    public String getCursor() { return cursor; }
}
