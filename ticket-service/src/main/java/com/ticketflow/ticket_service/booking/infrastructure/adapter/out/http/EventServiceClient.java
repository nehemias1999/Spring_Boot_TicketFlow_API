package com.ticketflow.ticket_service.booking.infrastructure.adapter.out.http;

import com.ticketflow.ticket_service.booking.application.dto.external.EventDto;
import com.ticketflow.ticket_service.booking.domain.exception.EventFullException;
import com.ticketflow.ticket_service.booking.domain.exception.EventNotFoundException;
import com.ticketflow.ticket_service.booking.domain.port.out.IEventServicePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * HTTP adapter that calls event-service to retrieve and update event data.
 * Forwards {@code X-Correlation-Id} from the current request to preserve
 * distributed tracing context across service boundaries.
 * All internal calls are authenticated with {@code X-Internal-Api-Key}.
 *
 * @author TicketFlow Team
 */
@Slf4j
@Component
public class EventServiceClient implements IEventServicePort {

    private final RestClient restClient;
    private final String internalApiKey;

    public EventServiceClient(
            @Value("${services.event-service.base-url}") String baseUrl,
            @Value("${services.internal-api-key}") String internalApiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.internalApiKey = internalApiKey;
    }

    @Override
    public List<String> getSellerEventIds(String sellerId) {
        try {
            List<String> ids = restClient.get()
                    .uri("/api/v1/events/my-event-ids")
                    .header("X-User-Id", sellerId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .header("X-Correlation-Id", getCorrelationId())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return ids != null ? ids : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch event IDs for sellerId '{}': {}", sellerId, e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public EventDto getEventById(String eventId) {
        try {
            return restClient.get()
                    .uri("/api/v1/events/{id}", eventId)
                    .header("X-Correlation-Id", getCorrelationId())
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new EventNotFoundException(eventId);
                    })
                    .body(EventDto.class);
        } catch (EventNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch event '{}': {}", eventId, e.getMessage());
            throw new EventNotFoundException(eventId);
        }
    }

    @Override
    public void decrementAvailableTickets(String eventId) {
        try {
            restClient.patch()
                    .uri("/api/v1/events/{id}/decrement-tickets", eventId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .header("X-Correlation-Id", getCorrelationId())
                    .retrieve()
                    .onStatus(status -> status.value() == 409, (req, res) -> {
                        throw new EventFullException(eventId);
                    })
                    .toBodilessEntity();
        } catch (EventFullException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to decrement tickets for event '{}': {}", eventId, e.getMessage());
            throw new EventFullException(eventId);
        }
    }

    @Override
    public void incrementAvailableTickets(String eventId) {
        try {
            restClient.patch()
                    .uri("/api/v1/events/{id}/increment-tickets", eventId)
                    .header("X-Internal-Api-Key", internalApiKey)
                    .header("X-Correlation-Id", getCorrelationId())
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Failed to increment tickets for event '{}': {}", eventId, e.getMessage());
        }
    }

    /**
     * Reads the {@code X-Correlation-Id} header from the current HTTP request.
     * Falls back to a new random UUID if the request context is not available.
     */
    private String getCorrelationId() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                String correlationId = attrs.getRequest().getHeader("X-Correlation-Id");
                if (correlationId != null && !correlationId.isBlank()) {
                    return correlationId;
                }
            }
        } catch (Exception ignored) {
        }
        return UUID.randomUUID().toString();
    }
}
