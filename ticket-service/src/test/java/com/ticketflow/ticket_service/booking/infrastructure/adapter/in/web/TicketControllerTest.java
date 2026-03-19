package com.ticketflow.ticket_service.booking.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.ticket_service.booking.application.dto.request.CreateTicketRequest;
import com.ticketflow.ticket_service.booking.application.dto.request.UpdateTicketRequest;
import com.ticketflow.ticket_service.booking.application.dto.response.TicketResponse;
import com.ticketflow.ticket_service.booking.domain.exception.TicketAlreadyCancelledException;
import com.ticketflow.ticket_service.booking.domain.exception.TicketNotFoundException;
import com.ticketflow.ticket_service.booking.domain.model.TicketStatus;
import com.ticketflow.ticket_service.booking.domain.port.in.ITicketService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link TicketController} using the Spring MVC test slice.
 */
@WebMvcTest(TicketController.class)
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@DisplayName("TicketController — unit tests")
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ITicketService ticketService;

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static TicketResponse buildResponse(String id, TicketStatus status) {
        return new TicketResponse(id, "EVT-001", "user-001",
                LocalDateTime.now(), status, LocalDateTime.now(), null);
    }

    private static CreateTicketRequest buildCreateRequest() {
        return new CreateTicketRequest("EVT-001", "user-001");
    }

    private static UpdateTicketRequest buildUpdateRequest() {
        return new UpdateTicketRequest("user-002");
    }

    // -------------------------------------------------------------------------
    // POST /api/v1/tickets
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /api/v1/tickets")
    class Create {

        @Test
        @DisplayName("should return 201 Created with TicketResponse body on success")
        void create_success_returns201() throws Exception {
            CreateTicketRequest request = buildCreateRequest();
            TicketResponse response = buildResponse("generated-uuid", TicketStatus.CONFIRMED);

            when(ticketService.create(any())).thenReturn(response);

            mockMvc.perform(post("/api/v1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value("generated-uuid"))
                    .andExpect(jsonPath("$.eventId").value("EVT-001"))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("should return 400 Bad Request when request body fails validation")
        void create_validationError_returns400() throws Exception {
            String invalidBody = """
                    {
                      "eventId": "",
                      "userId": ""
                    }
                    """;

            mockMvc.perform(post("/api/v1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/tickets/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/tickets/{id}")
    class GetById {

        @Test
        @DisplayName("should return 200 OK with TicketResponse when ticket is found")
        void getById_success_returns200() throws Exception {
            TicketResponse response = buildResponse("TKT-001", TicketStatus.CONFIRMED);
            when(ticketService.getById("TKT-001")).thenReturn(response);

            mockMvc.perform(get("/api/v1/tickets/TKT-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("TKT-001"))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("should return 404 Not Found when ticket does not exist")
        void getById_notFound_returns404() throws Exception {
            when(ticketService.getById("TKT-999")).thenThrow(new TicketNotFoundException("TKT-999"));

            mockMvc.perform(get("/api/v1/tickets/TKT-999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    // -------------------------------------------------------------------------
    // GET /api/v1/tickets
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /api/v1/tickets")
    class GetAll {

        @Test
        @DisplayName("should return 200 OK with paginated TicketResponse list using default params")
        void getAll_defaultParams_returns200() throws Exception {
            TicketResponse response = buildResponse("TKT-001", TicketStatus.CONFIRMED);
            Page<TicketResponse> page = new PageImpl<>(List.of(response));

            when(ticketService.getAll(any(), any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/tickets"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("TKT-001"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 200 OK with correct pagination when custom page/size is provided")
        void getAll_customPagination_returns200() throws Exception {
            Page<TicketResponse> emptyPage = new PageImpl<>(List.of());
            when(ticketService.getAll(any(), any(), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/tickets")
                            .param("page", "2")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("should cap page size at 100 when size exceeds the maximum")
        void getAll_cappedSize_returns200() throws Exception {
            Page<TicketResponse> emptyPage = new PageImpl<>(List.of());
            when(ticketService.getAll(any(), any(), any(), any())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/tickets").param("size", "999"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 200 OK when eventId, userId, and status filter params are provided")
        void getAll_withFilters_returns200() throws Exception {
            TicketResponse response = buildResponse("TKT-001", TicketStatus.CONFIRMED);
            Page<TicketResponse> page = new PageImpl<>(List.of(response));

            when(ticketService.getAll(any(), any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/v1/tickets")
                            .param("eventId", "EVT-001")
                            .param("userId", "user-001")
                            .param("status", "CONFIRMED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("TKT-001"));
        }
    }

    // -------------------------------------------------------------------------
    // PUT /api/v1/tickets/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /api/v1/tickets/{id}")
    class Update {

        @Test
        @DisplayName("should return 200 OK with updated TicketResponse on success")
        void update_success_returns200() throws Exception {
            UpdateTicketRequest request = buildUpdateRequest();
            TicketResponse response = new TicketResponse(
                    "TKT-001", "EVT-001", "user-002",
                    LocalDateTime.now(), TicketStatus.CONFIRMED, LocalDateTime.now(), LocalDateTime.now());

            when(ticketService.update(eq("TKT-001"), any())).thenReturn(response);

            mockMvc.perform(put("/api/v1/tickets/TKT-001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("TKT-001"))
                    .andExpect(jsonPath("$.userId").value("user-002"));
        }

        @Test
        @DisplayName("should return 404 Not Found when ticket to update does not exist")
        void update_notFound_returns404() throws Exception {
            UpdateTicketRequest request = buildUpdateRequest();

            when(ticketService.update(eq("TKT-999"), any()))
                    .thenThrow(new TicketNotFoundException("TKT-999"));

            mockMvc.perform(put("/api/v1/tickets/TKT-999")
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
                      "userId": ""
                    }
                    """;

            mockMvc.perform(put("/api/v1/tickets/TKT-001")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400));
        }
    }

    // -------------------------------------------------------------------------
    // PATCH /api/v1/tickets/{id}/cancel
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("PATCH /api/v1/tickets/{id}/cancel")
    class Cancel {

        @Test
        @DisplayName("should return 200 OK with CANCELLED status on success")
        void cancel_success_returns200() throws Exception {
            TicketResponse response = buildResponse("TKT-001", TicketStatus.CANCELLED);
            when(ticketService.cancel("TKT-001")).thenReturn(response);

            mockMvc.perform(patch("/api/v1/tickets/TKT-001/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("TKT-001"))
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("should return 404 Not Found when ticket to cancel does not exist")
        void cancel_notFound_returns404() throws Exception {
            when(ticketService.cancel("TKT-999")).thenThrow(new TicketNotFoundException("TKT-999"));

            mockMvc.perform(patch("/api/v1/tickets/TKT-999/cancel"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("should return 409 Conflict when ticket is already cancelled")
        void cancel_alreadyCancelled_returns409() throws Exception {
            when(ticketService.cancel("TKT-001"))
                    .thenThrow(new TicketAlreadyCancelledException("TKT-001"));

            mockMvc.perform(patch("/api/v1/tickets/TKT-001/cancel"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409))
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    // -------------------------------------------------------------------------
    // DELETE /api/v1/tickets/{id}
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /api/v1/tickets/{id}")
    class Delete {

        @Test
        @DisplayName("should return 204 No Content when ticket is soft-deleted successfully")
        void delete_success_returns204() throws Exception {
            doNothing().when(ticketService).delete("TKT-001");

            mockMvc.perform(delete("/api/v1/tickets/TKT-001"))
                    .andExpect(status().isNoContent());

            verify(ticketService).delete("TKT-001");
        }

        @Test
        @DisplayName("should return 404 Not Found when ticket to delete does not exist")
        void delete_notFound_returns404() throws Exception {
            doThrow(new TicketNotFoundException("TKT-999")).when(ticketService).delete("TKT-999");

            mockMvc.perform(delete("/api/v1/tickets/TKT-999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
