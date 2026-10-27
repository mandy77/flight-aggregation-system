package com.ota.flights.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Immutable domain entity representing a single flight leg.
 * Pure domain – no framework annotations.
 */
public final class FlightSegment {

    private final String airlineCode;
    private final String flightNumber;
    private final String origin;
    private final String destination;
    private final LocalDateTime departureTime;
    private final LocalDateTime arrivalTime;
    private final int durationMinutes;
    private final String aircraftType;

    public FlightSegment(
            String airlineCode,
            String flightNumber,
            String origin,
            String destination,
            LocalDateTime departureTime,
            LocalDateTime arrivalTime,
            int durationMinutes,
            String aircraftType) {

        if (airlineCode == null || airlineCode.isBlank()) {
            throw new IllegalArgumentException("Airline code is required");
        }
        if (flightNumber == null || flightNumber.isBlank()) {
            throw new IllegalArgumentException("Flight number is required");
        }
        if (origin == null || origin.length() != 3 || destination == null || destination.length() != 3) {
            throw new IllegalArgumentException("Airport codes must be 3-letter IATA codes");
        }
        if (departureTime == null || arrivalTime == null || !departureTime.isBefore(arrivalTime)) {
            throw new IllegalArgumentException("Departure must be before arrival");
        }

        this.airlineCode = airlineCode.toUpperCase();
        this.flightNumber = flightNumber;
        this.origin = origin.toUpperCase();
        this.destination = destination.toUpperCase();
        this.departureTime = departureTime;
        this.arrivalTime = arrivalTime;
        this.durationMinutes = durationMinutes;
        this.aircraftType = aircraftType;
    }

    /** Canonical key used for cross-provider segment matching. */
    public String identityKey() {
        return airlineCode + ":" + flightNumber + ":" + origin + ":" + destination + ":" + departureTime;
    }

    public String getAirlineCode() { return airlineCode; }
    public String getFlightNumber() { return flightNumber; }
    public String getOrigin() { return origin; }
    public String getDestination() { return destination; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getAircraftType() { return aircraftType; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlightSegment that)) return false;
        return Objects.equals(identityKey(), that.identityKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityKey());
    }
}
