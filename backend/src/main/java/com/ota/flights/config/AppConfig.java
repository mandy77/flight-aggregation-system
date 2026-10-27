package com.ota.flights.config;

import com.ota.flights.application.IngestFlightsUseCase;
import com.ota.flights.application.SearchFlightsUseCase;
import com.ota.flights.domain.port.CachePort;
import com.ota.flights.domain.port.FlightProviderPort;
import com.ota.flights.domain.port.ItineraryRepositoryPort;
import com.ota.flights.domain.service.AggregationService;
import com.ota.flights.infrastructure.provider.AlphaProvider;
import com.ota.flights.infrastructure.provider.BetaProvider;
import com.ota.flights.infrastructure.provider.GammaProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class AppConfig {

    @Bean
    public AggregationService aggregationService() {
        return new AggregationService();
    }

    @Bean
    public List<FlightProviderPort> flightProviders() {
        return List.of(
                new AlphaProvider(500),
                new BetaProvider(400),
                new GammaProvider(500)
        );
    }

    @Bean
    public IngestFlightsUseCase ingestFlightsUseCase(
            List<FlightProviderPort> providers,
            ItineraryRepositoryPort repository,
            AggregationService aggregation,
            CachePort cache) {
        return new IngestFlightsUseCase(providers, repository, aggregation, cache);
    }

    @Bean
    public SearchFlightsUseCase searchFlightsUseCase(ItineraryRepositoryPort repository) {
        return new SearchFlightsUseCase(repository);
    }
}
