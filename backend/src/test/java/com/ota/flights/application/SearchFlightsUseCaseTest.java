package com.ota.flights.application;

import com.ota.flights.domain.model.SearchCriteria;
import com.ota.flights.domain.model.SearchResult;
import com.ota.flights.domain.port.ItineraryRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class SearchFlightsUseCaseTest {

    @Test
    void execute_delegatesToRepository() {
        ItineraryRepositoryPort repository = mock(ItineraryRepositoryPort.class);
        SearchFlightsUseCase useCase = new SearchFlightsUseCase(repository);

        SearchCriteria criteria = new SearchCriteria(null, null, null, null, null, null, 20, null);
        SearchResult expected = new SearchResult(List.of(), null, false);
        when(repository.search(criteria)).thenReturn(expected);

        SearchResult actual = useCase.execute(criteria);

        assertThat(actual).isSameAs(expected);
        verify(repository).search(criteria);
        verifyNoMoreInteractions(repository);
    }
}