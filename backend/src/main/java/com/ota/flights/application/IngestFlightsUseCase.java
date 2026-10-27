package com.ota.flights.application;

import com.ota.flights.domain.model.Itinerary;
import com.ota.flights.domain.model.PricedItinerary;
import com.ota.flights.domain.port.CachePort;
import com.ota.flights.domain.port.FlightProviderPort;
import com.ota.flights.domain.port.ItineraryRepositoryPort;
import com.ota.flights.domain.service.AggregationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Concurrently query all providers, aggregate, deduplicate, price, and persist.
 * Resilient to individual provider failures.
 */
public class IngestFlightsUseCase {

    private static final Logger log = LoggerFactory.getLogger(IngestFlightsUseCase.class);

    private final List<FlightProviderPort> providers;
    private final ItineraryRepositoryPort repository;
    private final AggregationService aggregation;
    private final CachePort cache;
    private final ExecutorService executor;

    public IngestFlightsUseCase(
            List<FlightProviderPort> providers,
            ItineraryRepositoryPort repository,
            AggregationService aggregation,
            CachePort cache) {
        this.providers = providers;
        this.repository = repository;
        this.aggregation = aggregation;
        this.cache = cache;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    public Map<String, Object> execute(String origin, String destination, String departureDate) {
        List<CompletableFuture<ProviderResult>> futures = providers.stream()
                .map(p -> CompletableFuture.supplyAsync(
                        () -> safeFetch(p, origin, destination, departureDate), executor))
                .toList();

        List<Itinerary> all = new ArrayList<>();
        List<String> ok = new ArrayList<>();
        List<Map<String, String>> failed = new ArrayList<>();

        for (CompletableFuture<ProviderResult> f : futures) {
            ProviderResult r = f.join();
            if (r.error() != null) {
                failed.add(Map.of("provider", r.name(), "error", r.error()));
                log.warn("Provider {} failed: {}", r.name(), r.error());
            } else {
                ok.add(r.name());
                all.addAll(r.itineraries());
                log.info("Provider {} returned {} itineraries", r.name(), r.itineraries().size());
            }
        }

        List<PricedItinerary> priced = aggregation.aggregate(all);

        repository.clear();
        int written = repository.saveMany(priced);

        if (cache != null) {
            cache.deletePattern("search:*");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("ingested", written);
        result.put("rawCollected", all.size());
        result.put("afterDedupAndPricing", priced.size());
        result.put("providersOk", ok);
        result.put("providersFailed", failed);
        return result;
    }

    private ProviderResult safeFetch(
            FlightProviderPort provider, String origin, String destination, String departureDate) {
        try {
            List<Itinerary> list = provider.search(origin, destination, departureDate);
            return new ProviderResult(provider.getName(), list, null);
        } catch (Exception e) {
            return new ProviderResult(provider.getName(), List.of(), e.getMessage());
        }
    }

    private record ProviderResult(String name, List<Itinerary> itineraries, String error) {}
}
