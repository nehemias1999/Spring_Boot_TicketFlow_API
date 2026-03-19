package com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence;

import com.ticketflow.event_service.catalog.domain.model.Event;
import com.ticketflow.event_service.catalog.infrastructure.adapter.out.persistence.mapper.IEventPersistenceMapper;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link EventPersistenceAdapter}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EventPersistenceAdapter — unit tests")
class EventPersistenceAdapterTest {

    @Mock
    private IEventJpaRepository eventJpaRepository;

    @Mock
    private IEventPersistenceMapper eventPersistenceMapper;

    @InjectMocks
    private EventPersistenceAdapter eventPersistenceAdapter;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Event buildDomain(String id) {
        return Event.builder()
                .id(id)
                .title("Test Event")
                .description("Test description")
                .date("2026-10-15 20:00")
                .location("Test Location")
                .basePrice(BigDecimal.valueOf(100.00))
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private static EventEntity buildEntity(String id) {
        return EventEntity.builder()
                .id(id)
                .title("Test Event")
                .description("Test description")
                .date("2026-10-15 20:00")
                .location("Test Location")
                .basePrice(BigDecimal.valueOf(100.00))
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
            Event domain = buildDomain("EVT-001");
            EventEntity entity = buildEntity("EVT-001");

            when(eventPersistenceMapper.toEntity(domain)).thenReturn(entity);
            when(eventJpaRepository.save(entity)).thenReturn(entity);
            when(eventPersistenceMapper.toDomain(entity)).thenReturn(domain);

            // when
            Event result = eventPersistenceAdapter.save(domain);

            // then
            assertThat(result).isEqualTo(domain);
            verify(eventJpaRepository).save(entity);
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
            Event domain = buildDomain("EVT-001");
            EventEntity entity = buildEntity("EVT-001");

            when(eventJpaRepository.findByIdAndDeletedFalse("EVT-001"))
                    .thenReturn(Optional.of(entity));
            when(eventPersistenceMapper.toDomain(entity)).thenReturn(domain);

            // when
            Optional<Event> result = eventPersistenceAdapter.findByIdAndDeletedFalse("EVT-001");

            // then
            assertThat(result).isPresent().contains(domain);
        }

        @Test
        @DisplayName("should return empty Optional when entity is not found")
        void findByIdAndDeletedFalse_notFound() {
            // given
            when(eventJpaRepository.findByIdAndDeletedFalse("EVT-999"))
                    .thenReturn(Optional.empty());

            // when
            Optional<Event> result = eventPersistenceAdapter.findByIdAndDeletedFalse("EVT-999");

            // then
            assertThat(result).isEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // findAllByDeletedFalse()
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("findAllByDeletedFalse()")
    class FindAllByDeletedFalse {

        @Test
        @DisplayName("should return page of domain objects when active events exist")
        void findAllByDeletedFalse_success() {
            // given
            PageRequest pageable = PageRequest.of(0, 10);
            Event domain = buildDomain("EVT-001");
            EventEntity entity = buildEntity("EVT-001");
            Page<EventEntity> entityPage = new PageImpl<>(List.of(entity));

            when(eventJpaRepository.findAllByDeletedFalse(pageable)).thenReturn(entityPage);
            when(eventPersistenceMapper.toDomain(entity)).thenReturn(domain);

            // when
            Page<Event> result = eventPersistenceAdapter.findAllByDeletedFalse(pageable);

            // then
            assertThat(result.getContent()).containsExactly(domain);
            assertThat(result.getTotalElements()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return empty page when no active events exist")
        void findAllByDeletedFalse_empty() {
            // given
            PageRequest pageable = PageRequest.of(0, 10);
            when(eventJpaRepository.findAllByDeletedFalse(pageable)).thenReturn(Page.empty());

            // when
            Page<Event> result = eventPersistenceAdapter.findAllByDeletedFalse(pageable);

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
            Event domain = buildDomain("EVT-001");
            EventEntity entity = buildEntity("EVT-001");

            when(eventPersistenceMapper.toEntity(domain)).thenReturn(entity);
            when(eventJpaRepository.save(entity)).thenReturn(entity);
            when(eventPersistenceMapper.toDomain(entity)).thenReturn(domain);

            // when
            Event result = eventPersistenceAdapter.update(domain);

            // then
            assertThat(result).isEqualTo(domain);
            verify(eventJpaRepository).save(entity);
        }
    }
}
