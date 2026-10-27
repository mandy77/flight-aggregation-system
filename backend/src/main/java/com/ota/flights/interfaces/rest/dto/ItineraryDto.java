package com.ota.flights.interfaces.rest.dto;

import java.util.List;

public record ItineraryDto(
        String id,
        String origin,
        String destination,
        String departure,
        String arrival,
        int stops,
        int durationMinutes,
        List<String> carriers,
        List<String> flightNumbers,
        double basePrice,
        double finalPrice,
        String currency,
        String provider,
        String cabinClass,
        int seatsAvailable,
        List<SegmentDto> segments
) {}
