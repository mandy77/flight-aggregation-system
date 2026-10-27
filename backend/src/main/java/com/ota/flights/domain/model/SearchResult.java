package com.ota.flights.domain.model;

import java.util.List;

/**
 * Keyset-paginated search response.
 */
public final class SearchResult {

    private final List<PricedItinerary> items;
    private final String nextCursor;
    private final boolean hasMore;

    public SearchResult(List<PricedItinerary> items, String nextCursor, boolean hasMore) {
        this.items = List.copyOf(items);
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
    }

    public List<PricedItinerary> getItems() { return items; }
    public String getNextCursor() { return nextCursor; }
    public boolean isHasMore() { return hasMore; }
}
