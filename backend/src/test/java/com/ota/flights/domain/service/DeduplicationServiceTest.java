package com.ota.flights.domain.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

class DeduplicationServiceTest {

    @Test
    void deduplicate_emptyInput_returnsEmpty() {
        DeduplicationService service = new DeduplicationService();
        assertThat(service.deduplicate(List.of())).isEmpty();
    }
}