package com.ota.flights.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ota.flights.application.IngestFlightsUseCase;
import com.ota.flights.application.SearchFlightsUseCase;
import com.ota.flights.domain.model.SearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class FlightControllerTest {

    private MockMvc mvc;

    @Mock
    private IngestFlightsUseCase ingestUseCase;

    @Mock
    private SearchFlightsUseCase searchUseCase;

    @InjectMocks
    private FlightController controller;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .build();
    }

    @Test
    void health_returnsOk() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void ingest_returnsMappedBody() throws Exception {
        when(ingestUseCase.execute(any(), any(), any())).thenReturn(Map.of(
                "ingested", 1,
                "rawCollected", 2,
                "afterDedupAndPricing", 1,
                "providersOk", List.of("Mock-Alpha"),
                "providersFailed", List.of()
        ));

        mvc.perform(post("/ingest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ingested").value(1))
                .andExpect(jsonPath("$.rawCollected").value(2))
                .andExpect(jsonPath("$.afterDedupAndPricing").value(1));
    }

    @Test
    void search_returnsResponse() throws Exception {
        when(searchUseCase.execute(any())).thenReturn(new SearchResult(List.of(), null, false));

        mvc.perform(get("/search").param("limit", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }
}