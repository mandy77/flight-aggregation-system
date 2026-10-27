package com.ota.flights.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

class AggregationServiceTest {

    @Test
    void aggregate_returnsNonNullResult_forEmptyInput() {
        AggregationService service = new AggregationService(new DeduplicationService(), new PricingService(), new ValidationService());
        assertThat(service.aggregate(List.of())).isNotNull();
    }
}