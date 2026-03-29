package com.ticketflow.ticket_service.booking.infrastructure.adapter.out.http;

import com.ticketflow.ticket_service.booking.domain.port.out.IEventServicePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;

/**
 * HTTP adapter that calls event-service to retrieve a SELLER's event IDs.
 * <p>
 * Degrades gracefully to an empty list on any failure, so a transient
 * event-service outage does not break the ticket listing endpoint.
 * </p>
 *
 * @author TicketFlow Team
 */
@Slf4j
@Component
public class EventServiceClient implements IEventServicePort {

    private final RestClient restClient;

    public EventServiceClient(@Value("${services.event-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public List<String> getSellerEventIds(String sellerId) {
        try {
            List<String> ids = restClient.get()
                    .uri("/api/v1/events/my-event-ids")
                    .header("X-User-Id", sellerId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return ids != null ? ids : Collections.emptyList();
        } catch (Exception e) {
            log.warn("Failed to fetch event IDs for sellerId '{}': {}", sellerId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
