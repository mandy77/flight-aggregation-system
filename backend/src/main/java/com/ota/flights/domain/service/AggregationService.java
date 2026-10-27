package com.ota.flights.domain.service;

import com.ota.flights.domain.model.Itinerary;
import com.ota.flights.domain.model.PricedItinerary;

import java.util.Comparator;
import java.util.List;

/**
 * Orchestrates the full domain pipeline:
 * 1. Validate connections
 * 2. Deduplicate across providers
 * 3. Apply OTA markup
 * 4. Sort by final price
 */
public class AggregationService {

    private final DeduplicationService dedup;
    private final PricingService pricing;
    private final ValidationService validation;

    public AggregationService() {
        this(new DeduplicationService(), new PricingService(), new ValidationService());
    }

    public AggregationService(
            DeduplicationService dedup,
            PricingService pricing,
            ValidationService validation) {
        this.dedup = dedup;
        this.pricing = pricing;
        this.validation = validation;
    }

    public List<PricedItinerary> aggregate(List<Itinerary> rawItineraries) {
        List<Itinerary> valid = validation.filterValid(rawItineraries);
        List<Itinerary> unique = dedup.deduplicate(valid);
        List<PricedItinerary> priced = pricing.applyMarkup(unique);

        priced.sort(Comparator
                .comparing(PricedItinerary::getFinalPrice)
                .thenComparing(p -> p.getItinerary().getDepartureTime()));

        return priced;
    }
}
