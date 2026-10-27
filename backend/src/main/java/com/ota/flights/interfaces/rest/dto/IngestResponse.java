package com.ota.flights.interfaces.rest.dto;

import java.util.List;
import java.util.Map;

public record IngestResponse(
        int ingested,
        int rawCollected,
        int afterDedupAndPricing,
        List<String> providersOk,
        List<Map<String, String>> providersFailed
) {}
