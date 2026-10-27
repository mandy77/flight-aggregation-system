package com.ota.flights.domain.service;

import com.ota.flights.domain.model.Itinerary;
import com.ota.flights.domain.model.PricedItinerary;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Applies OTA operational markup.
 */
public class PricingService {

    public static final BigDecimal DEFAULT_MARKUP = new BigDecimal("0.05");

    private final BigDecimal markupPercent;

    public PricingService() {
        this(DEFAULT_MARKUP);
    }

    public PricingService(BigDecimal markupPercent) {
        this.markupPercent = markupPercent;
    }

    public List<PricedItinerary> applyMarkup(List<Itinerary> itineraries) {
        return itineraries.stream()
                .map(itinerary -> itinerary.withMarkup(markupPercent))
                .collect(Collectors.toList());
    }
}
