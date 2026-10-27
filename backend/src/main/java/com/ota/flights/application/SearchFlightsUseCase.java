package com.ota.flights.application;

import com.ota.flights.domain.model.SearchCriteria;
import com.ota.flights.domain.model.SearchResult;
import com.ota.flights.domain.port.ItineraryRepositoryPort;

/**
 * Search stored itineraries with filters and keyset pagination.
 */
public class SearchFlightsUseCase {

    private final ItineraryRepositoryPort repository;

    public SearchFlightsUseCase(ItineraryRepositoryPort repository) {
        this.repository = repository;
    }

    public SearchResult execute(SearchCriteria criteria) {
        return repository.search(criteria);
    }
}
