package com.ticketflow.ticket_service.booking.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.ticket_service.booking.application.dto.request.CreateTicketRequest;
import com.ticketflow.ticket_service.booking.application.dto.response.TicketResponse;
import com.ticketflow.ticket_service.booking.domain.exception.TicketAlreadyCancelledException;
import com.ticketflow.ticket_service.booking.domain.exception.TicketNotFoundException;
import com.ticketflow.ticket_service.booking.domain.exception.TicketOwnershipException;
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
import static org.mockito.ArgumentMatchers.anyString;
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

    private static final String USER_ID = "user-001";
    private static final String USER_EMAIL = "user@test.com";

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
        return new TicketResponse(id, "EVT-001", USER_ID,
                LocalDateTime.now(), status, LocalDateTime.now(), null);
    }

    private static CreateTicketRequest buildCreateRequest() {
        return new CreateTicketRequest("EVT-001");
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

            when(ticketService.create(any(), anyString(), anyString())).thenReturn(response);

            mockMvc.perform(post("/api/v1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", USER_ID)
                            .header("X-User-Email", USER_EMAIL)
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
                      "eventId": ""
                    }
                    """;

            mockMvc.perform(post("/api/v1/tickets")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-User-Id", USER_ID)
                            .header("X-User-Email", USER_EMAIL)
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
        @DisplayName("should return 200 OK with TicketResponse when ticket is found and owned by the user")
        void getById_success_returns200() throws Exception {
            TicketResponse response = buildResponse("TKT-001", TicketStatus.CONFIRMED);
            when(ticketService.getById(eq("TKT-001"), anyString())).thenReturn(response);

            mockMvc.perform(get("/api/v1/tickets/TKT-001")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("TKT-001"))
                    .andExpect(jsonPath("$.status").value("CONFIRMED"));
        }

        @Test
        @DisplayName("should return 404 Not Found when ticket does not exist")
        void getById_notFound_returns404() throws Exception {
            when(ticketService.getById(eq("TKT-999"), anyString()))
                    .thenThrow(new TicketNotFoundException("TKT-999"));

            mockMvc.perform(get("/api/v1/tickets/TKT-999")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("should return 403 Forbidden when ticket belongs to a different user")
        void getById_differentUser_returns403() throws Exception {
            when(ticketService.getById(eq("TKT-001"), anyString()))
                    .thenThrow(new TicketOwnershipException("TKT-001"));

            mockMvc.perform(get("/api/v1/tickets/TKT-001")
                            .header("X-User-Id", "other-user"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.status").value(403));
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

            when(ticketService.getAll(any(), any(), any(), anyString())).thenReturn(page);

            mockMvc.perform(get("/api/v1/tickets")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("TKT-001"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("should return 200 OK with correct pagination when custom page/size is provided")
        void getAll_customPagination_returns200() throws Exception {
            Page<TicketResponse> emptyPage = new PageImpl<>(List.of());
            when(ticketService.getAll(any(), any(), any(), anyString())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/tickets")
                            .header("X-User-Id", USER_ID)
                            .param("page", "2")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("should cap page size at 100 when size exceeds the maximum")
        void getAll_cappedSize_returns200() throws Exception {
            Page<TicketResponse> emptyPage = new PageImpl<>(List.of());
            when(ticketService.getAll(any(), any(), any(), anyString())).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/tickets")
                            .header("X-User-Id", USER_ID)
                            .param("size", "999"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("should return 200 OK when eventId and status filter params are provided")
        void getAll_withFilters_returns200() throws Exception {
            TicketResponse response = buildResponse("TKT-001", TicketStatus.CONFIRMED);
            Page<TicketResponse> page = new PageImpl<>(List.of(response));

            when(ticketService.getAll(any(), any(), any(), anyString())).thenReturn(page);

            mockMvc.perform(get("/api/v1/tickets")
                            .header("X-User-Id", USER_ID)
                            .param("eventId", "EVT-001")
                            .param("status", "CONFIRMED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value("TKT-001"));
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
            when(ticketService.cancel(eq("TKT-001"), anyString())).thenReturn(response);

            mockMvc.perform(patch("/api/v1/tickets/TKT-001/cancel")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("TKT-001"))
                    .andExpect(jsonPath("$.status").value("CANCELLED"));
        }

        @Test
        @DisplayName("should return 404 Not Found when ticket to cancel does not exist")
        void cancel_notFound_returns404() throws Exception {
            when(ticketService.cancel(eq("TKT-999"), anyString()))
                    .thenThrow(new TicketNotFoundException("TKT-999"));

            mockMvc.perform(patch("/api/v1/tickets/TKT-999/cancel")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }

        @Test
        @DisplayName("should return 409 Conflict when ticket is already cancelled")
        void cancel_alreadyCancelled_returns409() throws Exception {
            when(ticketService.cancel(eq("TKT-001"), anyString()))
                    .thenThrow(new TicketAlreadyCancelledException("TKT-001"));

            mockMvc.perform(patch("/api/v1/tickets/TKT-001/cancel")
                            .header("X-User-Id", USER_ID))
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
            doNothing().when(ticketService).delete(eq("TKT-001"), anyString());

            mockMvc.perform(delete("/api/v1/tickets/TKT-001")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isNoContent());

            verify(ticketService).delete(eq("TKT-001"), anyString());
        }

        @Test
        @DisplayName("should return 404 Not Found when ticket to delete does not exist")
        void delete_notFound_returns404() throws Exception {
            doThrow(new TicketNotFoundException("TKT-999"))
                    .when(ticketService).delete(eq("TKT-999"), anyString());

            mockMvc.perform(delete("/api/v1/tickets/TKT-999")
                            .header("X-User-Id", USER_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404));
        }
    }
}
