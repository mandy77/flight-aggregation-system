package com.ota.flights.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Immutable complete journey composed of one or more segments.
 * Pure domain entity.
 */
public final class Itinerary {

    private final String id;
    private final List<FlightSegment> segments;
    private final BigDecimal basePrice;
    private final String currency;
    private final String provider;
    private final CabinClass cabinClass;
    private final int seatsAvailable;

    public Itinerary(
            String id,
            List<FlightSegment> segments,
            BigDecimal basePrice,
            String currency,
            String provider,
            CabinClass cabinClass,
            int seatsAvailable) {

        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("Itinerary must have at least one segment");
        }
        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Price cannot be negative");
        }

        this.id = id != null ? id : UUID.randomUUID().toString();
        this.segments = List.copyOf(segments);
        this.basePrice = basePrice;
        this.currency = currency != null ? currency : "USD";
        this.provider = provider != null ? provider : "";
        this.cabinClass = cabinClass != null ? cabinClass : CabinClass.ECONOMY;
        this.seatsAvailable = seatsAvailable;
    }

    public String getId() { return id; }
    public List<FlightSegment> getSegments() { return segments; }
    public BigDecimal getBasePrice() { return basePrice; }
    public String getCurrency() { return currency; }
    public String getProvider() { return provider; }
    public CabinClass getCabinClass() { return cabinClass; }
    public int getSeatsAvailable() { return seatsAvailable; }

    public String getOrigin() {
        return segments.getFirst().getOrigin();
    }

    public String getDestination() {
        return segments.getLast().getDestination();
    }

    public LocalDateTime getDepartureTime() {
        return segments.getFirst().getDepartureTime();
    }

    public LocalDateTime getArrivalTime() {
        return segments.getLast().getArrivalTime();
    }

    public int getTotalDurationMinutes() {
        return (int) Duration.between(getDepartureTime(), getArrivalTime()).toMinutes();
    }

    public int getStops() {
        return segments.size() - 1;
    }

    public List<String> getCarriers() {
        return segments.stream()
                .map(FlightSegment::getAirlineCode)
                .collect(Collectors.toList());
    }

    public List<String> getFlightNumbers() {
        return segments.stream()
                .map(s -> s.getAirlineCode() + s.getFlightNumber())
                .collect(Collectors.toList());
    }

    /**
     * Canonical identity for deduplication:
     * same airline(s), flight numbers, connections and departure times.
     */
    public String identityKey() {
        return segments.stream()
                .map(FlightSegment::identityKey)
                .collect(Collectors.joining("|"));
    }

    /**
     * Filters dead legs and connections with insufficient layover.
     */
    public boolean isValidConnection(int minConnectionMinutes) {
        if (segments.size() == 1) {
            return true;
        }
        for (int i = 0; i < segments.size() - 1; i++) {
            FlightSegment current = segments.get(i);
            FlightSegment next = segments.get(i + 1);

            if (!current.getDestination().equals(next.getOrigin())) {
                return false; // dead leg
            }
            long layover = Duration.between(current.getArrivalTime(), next.getDepartureTime()).toMinutes();
            if (layover < minConnectionMinutes) {
                return false;
            }
        }
        return true;
    }

    public PricedItinerary withMarkup(BigDecimal markupPercent) {
        BigDecimal finalPrice = basePrice
                .multiply(BigDecimal.ONE.add(markupPercent))
                .setScale(2, RoundingMode.HALF_UP);
        return new PricedItinerary(this, finalPrice, markupPercent);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Itinerary that)) return false;
        return Objects.equals(identityKey(), that.identityKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(identityKey());
    }
}
