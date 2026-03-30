package com.ticketflow.ticket_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.ticket_service.booking.application.dto.external.EventDto;
import com.ticketflow.ticket_service.booking.application.dto.request.CreateTicketRequest;
import com.ticketflow.ticket_service.booking.domain.port.out.IEventServicePort;
import com.ticketflow.ticket_service.booking.domain.port.out.ITicketEventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Concurrency tests verifying that simultaneous ticket purchases do not
 * oversell an event whose capacity is limited.
 *
 * <p>These tests use a real H2 in-memory database (same as integration tests)
 * and fire N concurrent HTTP requests to POST /api/v1/tickets.  The event-service
 * HTTP calls are mocked, but the ticket persistence and capacity tracking in
 * ticket-service run against the actual JPA layer.</p>
 *
 * <p><b>Important:</b> ticket-service's {@code TicketService.create()} calls
 * {@code eventServicePort.decrementAvailableTickets()} which is mocked here.
 * The real concurrency guard being tested is that the service does not create
 * more tickets than the event's reported available capacity.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application.properties")
@DisplayName("Ticket purchase — concurrency tests")
class TicketConcurrencyTest {

    private static final String EVENT_ID   = "EVT-CONCURRENCY-001";
    private static final int    CAPACITY   = 3;
    private static final int    THREADS    = 10;

    @MockBean
    private ITicketEventPublisher ticketEventPublisher;

    @MockBean
    private IEventServicePort eventServicePort;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUpMocks() {
        EventDto eventDto = new EventDto(
                EVENT_ID, "Concurrency Event", "Test",
                LocalDateTime.of(2027, 6, 1, 20, 0),
                "Test Arena", BigDecimal.valueOf(50.00),
                CAPACITY, CAPACITY, null,
                LocalDateTime.now(), null);

        when(eventServicePort.getEventById(EVENT_ID)).thenReturn(eventDto);
        // decrementAvailableTickets is a void; no real decrement happens in the mock
        doNothing().when(eventServicePort).decrementAvailableTickets(anyString());
        doNothing().when(eventServicePort).incrementAvailableTickets(anyString());
        doNothing().when(ticketEventPublisher).publishTicketPurchased(any(), any(), any());
    }

    @Test
    @DisplayName("should not create more tickets than event capacity under concurrent load")
    void concurrentPurchases_doNotExceedCapacity() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateTicketRequest(EVENT_ID));

        ExecutorService executor      = Executors.newFixedThreadPool(THREADS);
        CountDownLatch  startGate     = new CountDownLatch(1);
        AtomicInteger   created201    = new AtomicInteger(0);
        AtomicInteger   rejected      = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            final String userId = "concurrent-user-" + i;
            futures.add(executor.submit(() -> {
                try {
                    startGate.await(); // all threads start at the same moment
                    MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("X-User-Id",    userId)
                                    .header("X-User-Email", userId + "@test.com")
                                    .header("X-User-Role",  "USER")
                                    .content(body))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 201) created201.incrementAndGet();
                    else               rejected.incrementAndGet();
                } catch (Exception e) {
                    rejected.incrementAndGet();
                }
                return null;
            }));
        }

        startGate.countDown(); // release all threads simultaneously
        for (Future<?> f : futures) f.get();
        executor.shutdown();

        // Exactly CAPACITY requests should succeed; the rest must be rejected
        assertThat(created201.get())
                .as("Number of successful purchases must not exceed event capacity")
                .isLessThanOrEqualTo(CAPACITY);

        assertThat(created201.get() + rejected.get())
                .as("Total responses must equal total requests")
                .isEqualTo(THREADS);
    }

    @Test
    @DisplayName("each concurrent purchaser should receive a unique ticket ID")
    void concurrentPurchases_eachTicketHasUniqueId() throws Exception {
        String body = objectMapper.writeValueAsString(new CreateTicketRequest(EVENT_ID));

        ExecutorService executor  = Executors.newFixedThreadPool(CAPACITY);
        CountDownLatch  startGate = new CountDownLatch(1);
        List<String>    ticketIds = new ArrayList<>();

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < CAPACITY; i++) {
            final String userId = "unique-user-" + UUID.randomUUID().toString().substring(0, 8);
            futures.add(executor.submit(() -> {
                try {
                    startGate.await();
                    MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .header("X-User-Id",    userId)
                                    .header("X-User-Email", userId + "@test.com")
                                    .header("X-User-Role",  "USER")
                                    .content(body))
                            .andReturn();

                    if (result.getResponse().getStatus() == 201) {
                        String id = objectMapper.readTree(
                                result.getResponse().getContentAsString()).get("id").asText();
                        synchronized (ticketIds) { ticketIds.add(id); }
                    }
                } catch (Exception ignored) {}
                return null;
            }));
        }

        startGate.countDown();
        for (Future<?> f : futures) f.get();
        executor.shutdown();

        assertThat(ticketIds)
                .as("All successfully created tickets must have distinct IDs")
                .doesNotHaveDuplicates();
    }
}
