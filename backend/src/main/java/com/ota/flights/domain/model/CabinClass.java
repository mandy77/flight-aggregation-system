package com.ota.flights.domain.model;

public enum CabinClass {
    ECONOMY,
    PREMIUM_ECONOMY,
    BUSINESS,
    FIRST;

    public static CabinClass from(String value) {
        if (value == null) return ECONOMY;
        return CabinClass.valueOf(value.toUpperCase().replace('-', '_').replace(' ', '_'));
    }
}
