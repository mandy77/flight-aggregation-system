package com.ota.flights.interfaces.rest.dto;

public record SegmentDto(
        String airline,
        String flightNumber,
        String origin,
        String destination,
        String departure,
        String arrival,
        int durationMinutes,
        String aircraft
) {}
