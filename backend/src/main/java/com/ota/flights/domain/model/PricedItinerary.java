package com.ota.flights.domain.model;

import java.math.BigDecimal;

/**
 * Itinerary after OTA operational markup has been applied.
 */
public final class PricedItinerary {

    private final Itinerary itinerary;
    private final BigDecimal finalPrice;
    private final BigDecimal markupPercent;

    public PricedItinerary(Itinerary itinerary, BigDecimal finalPrice, BigDecimal markupPercent) {
        this.itinerary = itinerary;
        this.finalPrice = finalPrice;
        this.markupPercent = markupPercent;
    }

    public Itinerary getItinerary() { return itinerary; }
    public BigDecimal getFinalPrice() { return finalPrice; }
    public BigDecimal getMarkupPercent() { return markupPercent; }

    public String getId() {
        return itinerary.getId();
    }

    public String identityKey() {
        return itinerary.identityKey();
    }
}
