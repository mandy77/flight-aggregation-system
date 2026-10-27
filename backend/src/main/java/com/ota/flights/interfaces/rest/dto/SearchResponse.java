package com.ota.flights.interfaces.rest.dto;

import java.util.List;

public record SearchResponse(
        List<ItineraryDto> items,
        String nextCursor,
        boolean hasMore,
        int count
) {}
