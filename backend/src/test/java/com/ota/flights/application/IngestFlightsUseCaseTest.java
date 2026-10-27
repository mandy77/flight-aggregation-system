package com.ota.flights.application;

import com.ota.flights.domain.port.CachePort;
import com.ota.flights.domain.port.FlightProviderPort;
import com.ota.flights.domain.port.ItineraryRepositoryPort;
import com.ota.flights.domain.service.AggregationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class IngestFlightsUseCaseTest {

    @Test
    void execute_runsAggregationPersistsAndInvalidatesCache() {
        FlightProviderPort provider = mock(FlightProviderPort.class);
        ItineraryRepositoryPort repository = mock(ItineraryRepositoryPort.class);
        AggregationService aggregationService = mock(AggregationService.class);
        CachePort cache = mock(CachePort.class);

        when(provider.getName()).thenReturn("Mock-Alpha");
        when(aggregationService.aggregate(anyList())).thenReturn(List.of());

        IngestFlightsUseCase useCase = new IngestFlightsUseCase(List.of(provider), repository, aggregationService, cache);

        Map<String, Object> result = useCase.execute("JFK", "LAX", null);

        assertThat(result).containsKeys("ingested", "rawCollected", "afterDedupAndPricing", "providersOk", "providersFailed");
        verify(provider).search(eq("JFK"), eq("LAX"), isNull());
        verify(repository).saveMany(anyList());
        verify(cache).deletePattern("search:*");
    }

    @Test
    void execute_marksProviderFailure_whenProviderThrows() {
        FlightProviderPort badProvider = mock(FlightProviderPort.class);
        ItineraryRepositoryPort repository = mock(ItineraryRepositoryPort.class);
        AggregationService aggregationService = mock(AggregationService.class);
        CachePort cache = mock(CachePort.class);

        when(badProvider.getName()).thenReturn("Mock-Bad");
        when(badProvider.search(any(), any(), any())).thenThrow(new RuntimeException("boom"));
        when(aggregationService.aggregate(anyList())).thenReturn(List.of());

        IngestFlightsUseCase useCase = new IngestFlightsUseCase(List.of(badProvider), repository, aggregationService, cache);

        Map<String, Object> result = useCase.execute(null, null, null);

        assertThat(result).containsKey("providersFailed");
        verify(repository).saveMany(anyList());
        verify(cache).deletePattern("search:*");
    }
}