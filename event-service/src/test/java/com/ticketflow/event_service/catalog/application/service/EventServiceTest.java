package com.ticketflow.event_service.catalog.application.service;

import com.ticketflow.event_service.catalog.application.dto.request.CreateEventRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateEventRequest;
import com.ticketflow.event_service.catalog.application.dto.response.EventResponse;
import com.ticketflow.event_service.catalog.application.mapper.IEventApplicationMapper;
import com.ticketflow.event_service.catalog.domain.exception.EventNotFoundException;
import com.ticketflow.event_service.catalog.domain.model.Event;
import com.ticketflow.event_service.catalog.domain.port.out.IEventPersistencePort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EventService}.
 * <p>
 * All dependencies ({@link IEventPersistencePort} and {@link IEventApplicationMapper})
 * are mocked with Mockito so that only the service business logic is tested in isolation.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventService — unit tests")
class EventServiceTest {

    @Mock
    private IEventPersistencePort eventPersistencePort;

    @Mock
    private IEventApplicationMapper eventApplicationMapper;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventPersistencePort, eventApplicationMapper, new SimpleMeterRegistry());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Event buildEvent(String id) {
        return Event.builder()
                .id(id)
                .title("Test Event")
                .description("Test description")
                .date(LocalDateTime.of(2026, 10, 15, 20, 0))
                .capacity(100)
                .availableTickets(100)
                .location("Test Location")
                .basePrice(BigDecimal.valueOf(100.00))
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static EventResponse buildResponse(String id) {
        return new EventResponse(id, "Test Event", "Test description",
                LocalDateTime.of(2026, 10, 15, 20, 0), "Test Location",
                BigDecimal.valueOf(100.00), 100, 100, null, LocalDateTime.now(), null);
    }

    private static CreateEventRequest buildCreateRequest() {
        return new CreateEventRequest("Test Event", "Test description",
                LocalDateTime.of(2027, 1, 1, 12, 0), "Test Location",
                BigDecimal.valueOf(100.00), 100);
    }

    private static UpdateEventRequest buildUpdateRequest() {
        return new UpdateEventRequest("Updated Title", "Updated description",
                LocalDateTime.of(2027, 11, 20, 18, 0), "Updated Location",
                BigDecimal.valueOf(150.00), 200);
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("should generate a UUID id, persist, and return EventResponse")
        void create_success() {
            // given
            CreateEventRequest request = buildCreateRequest();
            Event domain = buildEvent("some-uuid");
            EventResponse response = buildResponse("some-uuid");

            when(eventApplicationMapper.toDomain(request)).thenReturn(domain);
            when(eventPersistencePort.save(any(Event.class))).thenReturn(domain);
            when(eventApplicationMapper.toResponse(domain)).thenReturn(response);

            // when
            EventResponse result = eventService.create(request, "user-1", "SELLER");

            // then
            assertThat(result).isEqualTo(response);
            verify(eventPersistencePort).save(any(Event.class));
        }
    }

    // -------------------------------------------------------------------------
    // getById()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("should return EventResponse when event is found by ID")
        void getById_success() {
            // given
            Event domain = buildEvent("EVT-001");
            EventResponse response = buildResponse("EVT-001");

            when(eventPersistencePort.findByIdAndDeletedFalse("EVT-001"))
                    .thenReturn(Optional.of(domain));
            when(eventApplicationMapper.toResponse(domain)).thenReturn(response);

            // when
            EventResponse result = eventService.getById("EVT-001");

            // then
            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event is not found")
        void getById_notFound_throwsException() {
            // given
            when(eventPersistencePort.findByIdAndDeletedFalse("EVT-999"))
                    .thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> eventService.getById("EVT-999"))
                    .isInstanceOf(EventNotFoundException.class)
                    .hasMessageContaining("EVT-999");
        }
    }

    // -------------------------------------------------------------------------
    // getAll()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("should return paginated EventResponse list when active events exist")
        void getAll_success() {
            // given
            PageRequest pageable = PageRequest.of(0, 10);
            Event domain = buildEvent("EVT-001");
            EventResponse response = buildResponse("EVT-001");
            Page<Event> domainPage = new PageImpl<>(List.of(domain));

            when(eventPersistencePort.findAllByFilters(null, null, pageable)).thenReturn(domainPage);
            when(eventApplicationMapper.toResponse(domain)).thenReturn(response);

            // when
            Page<EventResponse> result = eventService.getAll(null, null, pageable);

            // then
            assertThat(result.getContent()).containsExactly(response);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty page when no active events exist")
        void getAll_empty() {
            // given
            PageRequest pageable = PageRequest.of(0, 10);
            when(eventPersistencePort.findAllByFilters(null, null, pageable)).thenReturn(Page.empty());

            // when
            Page<EventResponse> result = eventService.getAll(null, null, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should forward title and location filters to persistence port")
        void getAll_withFilters() {
            // given
            PageRequest pageable = PageRequest.of(0, 10);
            Event domain = buildEvent("EVT-001");
            EventResponse response = buildResponse("EVT-001");
            Page<Event> domainPage = new PageImpl<>(List.of(domain));

            when(eventPersistencePort.findAllByFilters("Test", "Location", pageable)).thenReturn(domainPage);
            when(eventApplicationMapper.toResponse(domain)).thenReturn(response);

            // when
            Page<EventResponse> result = eventService.getAll("Test", "Location", pageable);

            // then
            assertThat(result.getContent()).containsExactly(response);
            verify(eventPersistencePort).findAllByFilters("Test", "Location", pageable);
        }
    }

    // -------------------------------------------------------------------------
    // update()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("should update and return EventResponse when event is found")
        void update_success() {
            // given
            UpdateEventRequest request = buildUpdateRequest();
            Event existing = buildEvent("EVT-001");
            existing.setCreatorId("user-1");
            EventResponse response = buildResponse("EVT-001");

            when(eventPersistencePort.findByIdAndDeletedFalse("EVT-001"))
                    .thenReturn(Optional.of(existing));
            doNothing().when(eventApplicationMapper).updateDomainFromRequest(eq(request), eq(existing));
            when(eventPersistencePort.update(existing)).thenReturn(existing);
            when(eventApplicationMapper.toResponse(existing)).thenReturn(response);

            // when
            EventResponse result = eventService.update("EVT-001", request, "user-1", "SELLER");

            // then
            assertThat(result).isEqualTo(response);
            verify(eventApplicationMapper).updateDomainFromRequest(request, existing);
            verify(eventPersistencePort).update(existing);
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event to update is not found")
        void update_notFound_throwsException() {
            // given
            UpdateEventRequest request = buildUpdateRequest();
            when(eventPersistencePort.findByIdAndDeletedFalse("EVT-999"))
                    .thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> eventService.update("EVT-999", request, "user-1", "SELLER"))
                    .isInstanceOf(EventNotFoundException.class)
                    .hasMessageContaining("EVT-999");

            verify(eventPersistencePort, never()).update(any());
        }
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("should set deleted flag to true and persist when event is found")
        void delete_success() {
            // given
            Event existing = buildEvent("EVT-001");
            existing.setCreatorId("user-1");
            when(eventPersistencePort.findByIdAndDeletedFalse("EVT-001"))
                    .thenReturn(Optional.of(existing));

            // when
            eventService.delete("EVT-001", "user-1", "SELLER");

            // then
            assertThat(existing.isDeleted()).isTrue();
            verify(eventPersistencePort).update(existing);
        }

        @Test
        @DisplayName("should throw EventNotFoundException when event to delete is not found")
        void delete_notFound_throwsException() {
            // given
            when(eventPersistencePort.findByIdAndDeletedFalse("EVT-999"))
                    .thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> eventService.delete("EVT-999", "user-1", "SELLER"))
                    .isInstanceOf(EventNotFoundException.class)
                    .hasMessageContaining("EVT-999");

            verify(eventPersistencePort, never()).update(any());
        }
    }
}
