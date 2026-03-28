package com.ticketflow.ticket_service.booking.application.service;

import com.ticketflow.ticket_service.booking.application.dto.request.CreateTicketRequest;
import com.ticketflow.ticket_service.booking.application.dto.response.TicketResponse;
import com.ticketflow.ticket_service.booking.application.mapper.ITicketApplicationMapper;
import com.ticketflow.ticket_service.booking.domain.exception.TicketAlreadyCancelledException;
import com.ticketflow.ticket_service.booking.domain.exception.TicketNotFoundException;
import com.ticketflow.ticket_service.booking.domain.exception.TicketOwnershipException;
import com.ticketflow.ticket_service.booking.domain.model.Ticket;
import com.ticketflow.ticket_service.booking.domain.model.TicketStatus;
import com.ticketflow.ticket_service.booking.domain.port.out.ITicketEventPublisher;
import com.ticketflow.ticket_service.booking.domain.port.out.ITicketPersistencePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TicketService}.
 * <p>
 * All dependencies ({@link ITicketPersistencePort} and {@link ITicketApplicationMapper})
 * are mocked with Mockito so that only the service business logic is tested in isolation.
 * </p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService — unit tests")
class TicketServiceTest {

    private static final String USER_ID = "user-001";
    private static final String USER_EMAIL = "user@test.com";

    @Mock
    private ITicketPersistencePort ticketPersistencePort;

    @Mock
    private ITicketApplicationMapper ticketApplicationMapper;

    @Mock
    private ITicketEventPublisher ticketEventPublisher;

    @InjectMocks
    private TicketService ticketService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Ticket buildTicket(String id, TicketStatus status) {
        return Ticket.builder()
                .id(id)
                .eventId("EVT-001")
                .userId(USER_ID)
                .purchaseDate(LocalDateTime.now())
                .status(status)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static TicketResponse buildResponse(String id, TicketStatus status) {
        return new TicketResponse(id, "EVT-001", USER_ID,
                LocalDateTime.now(), status, LocalDateTime.now(), null);
    }

    private static CreateTicketRequest buildCreateRequest() {
        return new CreateTicketRequest("EVT-001");
    }

    // -------------------------------------------------------------------------
    // create()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("should generate a UUID id, persist, publish event, and return TicketResponse")
        void create_success() {
            // given
            CreateTicketRequest request = buildCreateRequest();
            Ticket domain = buildTicket("some-uuid", TicketStatus.CONFIRMED);
            TicketResponse response = buildResponse("some-uuid", TicketStatus.CONFIRMED);

            when(ticketApplicationMapper.toDomain(request)).thenReturn(domain);
            when(ticketPersistencePort.save(any(Ticket.class))).thenReturn(domain);
            when(ticketApplicationMapper.toResponse(domain)).thenReturn(response);
            doNothing().when(ticketEventPublisher).publishTicketPurchased(any(), any(), any());

            // when
            TicketResponse result = ticketService.create(request, USER_ID, USER_EMAIL);

            // then
            assertThat(result).isEqualTo(response);
            verify(ticketPersistencePort).save(any(Ticket.class));
            verify(ticketEventPublisher).publishTicketPurchased(anyString(), eq(USER_ID), eq(USER_EMAIL));
        }
    }

    // -------------------------------------------------------------------------
    // getById()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getById()")
    class GetById {

        @Test
        @DisplayName("should return TicketResponse when ticket is found and owned by the authenticated user")
        void getById_success() {
            // given
            Ticket domain = buildTicket("TKT-001", TicketStatus.CONFIRMED);
            TicketResponse response = buildResponse("TKT-001", TicketStatus.CONFIRMED);

            when(ticketPersistencePort.findByIdAndDeletedFalse("TKT-001"))
                    .thenReturn(Optional.of(domain));
            when(ticketApplicationMapper.toResponse(domain)).thenReturn(response);

            // when
            TicketResponse result = ticketService.getById("TKT-001", USER_ID);

            // then
            assertThat(result).isEqualTo(response);
        }

        @Test
        @DisplayName("should throw TicketNotFoundException when ticket is not found")
        void getById_notFound_throwsException() {
            // given
            when(ticketPersistencePort.findByIdAndDeletedFalse("TKT-999"))
                    .thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> ticketService.getById("TKT-999", USER_ID))
                    .isInstanceOf(TicketNotFoundException.class)
                    .hasMessageContaining("TKT-999");
        }

        @Test
        @DisplayName("should throw TicketOwnershipException when ticket belongs to a different user")
        void getById_differentUser_throwsOwnershipException() {
            // given
            Ticket domain = buildTicket("TKT-001", TicketStatus.CONFIRMED);
            when(ticketPersistencePort.findByIdAndDeletedFalse("TKT-001"))
                    .thenReturn(Optional.of(domain));

            // when / then
            assertThatThrownBy(() -> ticketService.getById("TKT-001", "other-user"))
                    .isInstanceOf(TicketOwnershipException.class)
                    .hasMessageContaining("TKT-001");
        }
    }

    // -------------------------------------------------------------------------
    // getAll()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("should return paginated TicketResponse list filtered by the authenticated user")
        void getAll_success() {
            // given
            PageRequest pageable = PageRequest.of(0, 10);
            Ticket domain = buildTicket("TKT-001", TicketStatus.CONFIRMED);
            TicketResponse response = buildResponse("TKT-001", TicketStatus.CONFIRMED);
            Page<Ticket> domainPage = new PageImpl<>(List.of(domain));

            when(ticketPersistencePort.findAllByFilters(null, USER_ID, null, pageable)).thenReturn(domainPage);
            when(ticketApplicationMapper.toResponse(domain)).thenReturn(response);

            // when
            Page<TicketResponse> result = ticketService.getAll(null, null, pageable, USER_ID);

            // then
            assertThat(result.getContent()).containsExactly(response);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty page when authenticated user has no tickets")
        void getAll_empty() {
            // given
            PageRequest pageable = PageRequest.of(0, 10);
            when(ticketPersistencePort.findAllByFilters(null, USER_ID, null, pageable)).thenReturn(Page.empty());

            // when
            Page<TicketResponse> result = ticketService.getAll(null, null, pageable, USER_ID);

            // then
            assertThat(result.getContent()).isEmpty();
        }

        @Test
        @DisplayName("should forward eventId and status filters along with authenticatedUserId to persistence port")
        void getAll_withFilters() {
            // given
            PageRequest pageable = PageRequest.of(0, 10);
            Ticket domain = buildTicket("TKT-001", TicketStatus.CONFIRMED);
            TicketResponse response = buildResponse("TKT-001", TicketStatus.CONFIRMED);
            Page<Ticket> domainPage = new PageImpl<>(List.of(domain));

            when(ticketPersistencePort.findAllByFilters("EVT-001", USER_ID, "CONFIRMED", pageable))
                    .thenReturn(domainPage);
            when(ticketApplicationMapper.toResponse(domain)).thenReturn(response);

            // when
            Page<TicketResponse> result = ticketService.getAll("EVT-001", "CONFIRMED", pageable, USER_ID);

            // then
            assertThat(result.getContent()).containsExactly(response);
            verify(ticketPersistencePort).findAllByFilters("EVT-001", USER_ID, "CONFIRMED", pageable);
        }
    }

    // -------------------------------------------------------------------------
    // cancel()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("cancel()")
    class Cancel {

        @Test
        @DisplayName("should cancel ticket and return TicketResponse with CANCELLED status")
        void cancel_success() {
            // given
            Ticket existing = buildTicket("TKT-001", TicketStatus.CONFIRMED);
            Ticket cancelled = buildTicket("TKT-001", TicketStatus.CANCELLED);
            TicketResponse response = buildResponse("TKT-001", TicketStatus.CANCELLED);

            when(ticketPersistencePort.findByIdAndDeletedFalse("TKT-001"))
                    .thenReturn(Optional.of(existing));
            when(ticketPersistencePort.update(existing)).thenReturn(cancelled);
            when(ticketApplicationMapper.toResponse(cancelled)).thenReturn(response);

            // when
            TicketResponse result = ticketService.cancel("TKT-001", USER_ID);

            // then
            assertThat(result.status()).isEqualTo(TicketStatus.CANCELLED);
            assertThat(existing.getStatus()).isEqualTo(TicketStatus.CANCELLED);
            verify(ticketPersistencePort).update(existing);
        }

        @Test
        @DisplayName("should throw TicketNotFoundException when ticket to cancel is not found")
        void cancel_notFound_throwsException() {
            // given
            when(ticketPersistencePort.findByIdAndDeletedFalse("TKT-999"))
                    .thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> ticketService.cancel("TKT-999", USER_ID))
                    .isInstanceOf(TicketNotFoundException.class)
                    .hasMessageContaining("TKT-999");

            verify(ticketPersistencePort, never()).update(any());
        }

        @Test
        @DisplayName("should throw TicketAlreadyCancelledException when ticket is already cancelled")
        void cancel_alreadyCancelled_throwsException() {
            // given
            Ticket alreadyCancelled = buildTicket("TKT-001", TicketStatus.CANCELLED);
            when(ticketPersistencePort.findByIdAndDeletedFalse("TKT-001"))
                    .thenReturn(Optional.of(alreadyCancelled));

            // when / then
            assertThatThrownBy(() -> ticketService.cancel("TKT-001", USER_ID))
                    .isInstanceOf(TicketAlreadyCancelledException.class)
                    .hasMessageContaining("TKT-001");

            verify(ticketPersistencePort, never()).update(any());
        }
    }

    // -------------------------------------------------------------------------
    // delete()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("should set deleted flag to true and persist when ticket is found and owned by the authenticated user")
        void delete_success() {
            // given
            Ticket existing = buildTicket("TKT-001", TicketStatus.CONFIRMED);
            when(ticketPersistencePort.findByIdAndDeletedFalse("TKT-001"))
                    .thenReturn(Optional.of(existing));

            // when
            ticketService.delete("TKT-001", USER_ID);

            // then
            assertThat(existing.isDeleted()).isTrue();
            verify(ticketPersistencePort).update(existing);
        }

        @Test
        @DisplayName("should throw TicketNotFoundException when ticket to delete is not found")
        void delete_notFound_throwsException() {
            // given
            when(ticketPersistencePort.findByIdAndDeletedFalse("TKT-999"))
                    .thenReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> ticketService.delete("TKT-999", USER_ID))
                    .isInstanceOf(TicketNotFoundException.class)
                    .hasMessageContaining("TKT-999");

            verify(ticketPersistencePort, never()).update(any());
        }
    }
}
