package com.ota.flights.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

class PricingServiceTest {

    @Test
    void apply_emptyInput_returnsEmpty() {
        PricingService service = new PricingService();
        assertThat(service.applyMarkup(List.of())).isEmpty();
    }
}