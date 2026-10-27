package com.ota.flights.domain.port;

import com.ota.flights.domain.model.PricedItinerary;
import com.ota.flights.domain.model.SearchCriteria;
import com.ota.flights.domain.model.SearchResult;

import java.util.List;

/**
 * Port for persisting / querying aggregated itineraries.
 */
public interface ItineraryRepositoryPort {

    int saveMany(List<PricedItinerary> itineraries);

    SearchResult search(SearchCriteria criteria);

    long count(SearchCriteria criteria);

    void clear();
}
