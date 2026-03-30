package com.ticketflow.event_service.catalog.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.event_service.catalog.application.dto.request.CreateEventRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateEventRequest;
import com.ticketflow.event_service.catalog.application.dto.response.EventResponse;
import com.ticketflow.event_service.catalog.domain.exception.EventNotFoundException;
import com.ticketflow.event_service.catalog.domain.port.in.IEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link EventController} using the Spring MVC test slice.
 */
@WebMvcTest(EventController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@DisplayName("EventController — unit tests")
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IEventService eventService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

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
    // POST /api/v1/events
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/events")
    class Create {

        @Test
        @DisplayName("should return 201 Created with EventResponse body on success")
        void create_success_returns201() throws Exception {
            CreateEventRequest request = buildCreateRequest();
            EventResponse response = buildResponse("generated-uuid");

            when(eventService.create(any(), any(), any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/events")
                            .header("X-User-Id", "user-1")
                            .header("X-User-Role", "SELLER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("generated-uuid"))
                    .andExpect(jsonPath("$.title").value("Test Event"))
                    .andExpect(jsonPath("$.location").value("Test Location"));
        }

        @Test
        @DisplayName("should return 400 Bad Request when request body fails validation")
        void create_validationError_returns400() throws Exception {
            String invalidBody = """
                    {
                      "title": "a",
                      "description": "",
                      "date": null,
                      "location": "",
                      "basePrice": -1,
                      "capacity": 0
                    }
                    """;

            mockMvc.perform(post("/api/v1/events")
                            .header("X-User-Id", "user-1")
                            .header("X-User-Role", "SELLER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/events/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/events/{id}")
    class GetById {

        @Test
        @DisplayName("should return 200 OK with EventResponse when event is found")
        void getById_success_returns200() throws Exception {
            EventResponse response = buildResponse("EVT-001");
            when(eventService.getById("EVT-001")).thenReturn(response);

            mockMvc.perform(get("/api/v1/events/EVT-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("EVT-001"))
                    .andExpect(jsonPath("$.title").value("Test Event"));
        }

        @Test
        @DisplayName("should return 404 Not Found when event does not exist")
        void getById_notFound_returns404() throws Exception {
            when(eventService.getById("EVT-999")).thenThrow(new EventNotFoundException("EVT-999"));

            mockMvc.perform(get("/api/v1/events/EVT-999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/events
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/events")
    class GetAll {

        @Test
        @DisplayName("should return 200 OK with paginated EventResponse list using default params")
        void getAll_defaultParams_returns200() throws Exception {
            EventResponse response = buildResponse("EVT-001");
            Page<EventResponse> page = new PageImpl<>(List.of(response));

            when(eventService.getAll(any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("EVT-001"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 200 OK with correct pagination when custom page/size is provided")
        void getAll_customPagination_returns200() throws Exception {
            Page<EventResponse> emptyPage = new PageImpl<>(List.of());
            when(eventService.getAll(any(), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/events")
                            .param("page", "2")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("should return 400 Bad Request when size exceeds maximum of 100")
        void getAll_oversizedPage_returns400() throws Exception {
            mockMvc.perform(get("/api/v1/events").param("size", "999"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should return 200 OK when title and location filter params are provided")
        void getAll_withFilters_returns200() throws Exception {
            EventResponse response = buildResponse("EVT-001");
            Page<EventResponse> page = new PageImpl<>(List.of(response));

            when(eventService.getAll(any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/events")
                            .param("title", "Test")
                            .param("location", "Madrid"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("EVT-001"));
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/events/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/v1/events/{id}")
    class Update {

        @Test
        @DisplayName("should return 200 OK with updated EventResponse on success")
        void update_success_returns200() throws Exception {
            UpdateEventRequest request = buildUpdateRequest();
            EventResponse response = new EventResponse(
                    "EVT-001", "Updated Title", "Updated description",
                    LocalDateTime.of(2027, 11, 20, 18, 0), "Updated Location",
                    BigDecimal.valueOf(150.00), 200, 200, null, LocalDateTime.now(), LocalDateTime.now());

            when(eventService.update(eq("EVT-001"), any(), any(), any())).thenReturn(response);

            mockMvc.perform(put("/api/v1/events/EVT-001")
                            .header("X-User-Id", "user-1")
                            .header("X-User-Role", "SELLER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("EVT-001"))
                    .andExpect(jsonPath("$.title").value("Updated Title"));
        }

        @Test
        @DisplayName("should return 404 Not Found when event to update does not exist")
        void update_notFound_returns404() throws Exception {
            UpdateEventRequest request = buildUpdateRequest();

            when(eventService.update(eq("EVT-999"), any(), any(), any()))
                    .thenThrow(new EventNotFoundException("EVT-999"));

            mockMvc.perform(put("/api/v1/events/EVT-999")
                            .header("X-User-Id", "user-1")
                            .header("X-User-Role", "SELLER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("should return 400 Bad Request when update request body fails validation")
        void update_validationError_returns400() throws Exception {
            String invalidBody = """
                    {
                      "title": "",
                      "description": "",
                      "date": null,
                      "location": "",
                      "basePrice": -10,
                      "capacity": 0
                    }
                    """;

            mockMvc.perform(put("/api/v1/events/EVT-001")
                            .header("X-User-Id", "user-1")
                            .header("X-User-Role", "SELLER")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/events/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/v1/events/{id}")
    class Delete {

        @Test
        @DisplayName("should return 204 No Content when event is soft-deleted successfully")
        void delete_success_returns204() throws Exception {
            doNothing().when(eventService).delete(eq("EVT-001"), any(), any());

            mockMvc.perform(delete("/api/v1/events/EVT-001")
                            .header("X-User-Id", "user-1")
                            .header("X-User-Role", "SELLER"))
                    .andExpect(status().isNoContent());

            verify(eventService).delete(eq("EVT-001"), any(), any());
        }

        @Test
        @DisplayName("should return 404 Not Found when event to delete does not exist")
        void delete_notFound_returns404() throws Exception {
            doThrow(new EventNotFoundException("EVT-999")).when(eventService)
                    .delete(eq("EVT-999"), any(), any());

            mockMvc.perform(delete("/api/v1/events/EVT-999")
                            .header("X-User-Id", "user-1")
                            .header("X-User-Role", "SELLER"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
