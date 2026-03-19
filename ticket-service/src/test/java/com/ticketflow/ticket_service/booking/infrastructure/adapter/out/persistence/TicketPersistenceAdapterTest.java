package com.ticketflow.ticket_service.booking.infrastructure.adapter.out.persistence;

import com.ticketflow.ticket_service.booking.domain.model.Ticket;
import com.ticketflow.ticket_service.booking.domain.model.TicketStatus;
import com.ticketflow.ticket_service.booking.infrastructure.adapter.out.persistence.mapper.ITicketPersistenceMapper;
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
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TicketPersistenceAdapter}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TicketPersistenceAdapter — unit tests")
class TicketPersistenceAdapterTest {

    @Mock
    private ITicketJpaRepository ticketJpaRepository;

    @Mock
    private ITicketPersistenceMapper ticketPersistenceMapper;

    @InjectMocks
    private TicketPersistenceAdapter ticketPersistenceAdapter;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Ticket buildDomain(String id) {
        return Ticket.builder()
                .id(id)
                .eventId("EVT-001")
                .userId("user-001")
                .purchaseDate(LocalDateTime.now())
                .status(TicketStatus.CONFIRMED)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static TicketEntity buildEntity(String id) {
        return TicketEntity.builder()
                .id(id)
                .eventId("EVT-001")
                .userId("user-001")
                .purchaseDate(LocalDateTime.now())
                .status(TicketStatus.CONFIRMED)
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // save()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("save()")
    class Save {

        @Test
        @DisplayName("should convert to entity, persist, and return domain object")
        void save_success() {
            // given
            Ticket domain = buildDomain("TKT-001");
            TicketEntity entity = buildEntity("TKT-001");

            when(ticketPersistenceMapper.toEntity(domain)).thenReturn(entity);
            when(ticketJpaRepository.save(entity)).thenReturn(entity);
            when(ticketPersistenceMapper.toDomain(entity)).thenReturn(domain);

            // when
            Ticket result = ticketPersistenceAdapter.save(domain);

            // then
            assertThat(result).isEqualTo(domain);
            verify(ticketJpaRepository).save(entity);
        }
    }

    // -------------------------------------------------------------------------
    // findByIdAndDeletedFalse()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findByIdAndDeletedFalse()")
    class FindByIdAndDeletedFalse {

        @Test
        @DisplayName("should return Optional with domain object when entity is found")
        void findByIdAndDeletedFalse_found() {
            // given
            Ticket domain = buildDomain("TKT-001");
            TicketEntity entity = buildEntity("TKT-001");

            when(ticketJpaRepository.findByIdAndDeletedFalse("TKT-001"))
                    .thenReturn(Optional.of(entity));
            when(ticketPersistenceMapper.toDomain(entity)).thenReturn(domain);

            // when
            Optional<Ticket> result = ticketPersistenceAdapter.findByIdAndDeletedFalse("TKT-001");

            // then
            assertThat(result).isPresent().contains(domain);
        }

        @Test
        @DisplayName("should return empty Optional when entity is not found")
        void findByIdAndDeletedFalse_notFound() {
            // given
            when(ticketJpaRepository.findByIdAndDeletedFalse("TKT-999"))
                    .thenReturn(Optional.empty());

            // when
            Optional<Ticket> result = ticketPersistenceAdapter.findByIdAndDeletedFalse("TKT-999");

            // then
            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // findAllByFilters()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAllByFilters()")
    class FindAllByFilters {

        @Test
        @DisplayName("should return page of domain objects when tickets exist")
        void findAllByFilters_success() {
            // given
            PageRequest pageable = PageRequest.of(0, 10);
            Ticket domain = buildDomain("TKT-001");
            TicketEntity entity = buildEntity("TKT-001");
            Page<TicketEntity> entityPage = new PageImpl<>(List.of(entity));

            when(ticketJpaRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(entityPage);
            when(ticketPersistenceMapper.toDomain(entity)).thenReturn(domain);

            // when
            Page<Ticket> result = ticketPersistenceAdapter.findAllByFilters(null, null, null, pageable);

            // then
            assertThat(result.getContent()).containsExactly(domain);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty page when no tickets match the filters")
        void findAllByFilters_empty() {
            // given
            PageRequest pageable = PageRequest.of(0, 10);
            when(ticketJpaRepository.findAll(any(Specification.class), any(PageRequest.class)))
                    .thenReturn(Page.empty());

            // when
            Page<Ticket> result = ticketPersistenceAdapter.findAllByFilters(null, null, null, pageable);

            // then
            assertThat(result.getContent()).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // update()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("should convert to entity, merge via JPA save, and return updated domain")
        void update_success() {
            // given
            Ticket domain = buildDomain("TKT-001");
            TicketEntity entity = buildEntity("TKT-001");

            when(ticketPersistenceMapper.toEntity(domain)).thenReturn(entity);
            when(ticketJpaRepository.save(entity)).thenReturn(entity);
            when(ticketPersistenceMapper.toDomain(entity)).thenReturn(domain);

            // when
            Ticket result = ticketPersistenceAdapter.update(domain);

            // then
            assertThat(result).isEqualTo(domain);
            verify(ticketJpaRepository).save(entity);
        }
    }
}
