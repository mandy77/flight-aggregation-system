package com.ota.flights.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

class ValidationServiceTest {

    @Test
    void validate_emptyInput_returnsEmpty() {
        ValidationService service = new ValidationService();
        assertThat(service.filterValid(List.of())).isEmpty();
    }
}