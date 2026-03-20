package com.ticketflow.ticket_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.ticket_service.booking.application.dto.request.CreateTicketRequest;
import com.ticketflow.ticket_service.booking.application.dto.request.UpdateTicketRequest;
import com.ticketflow.ticket_service.booking.domain.port.out.ITicketEventPublisher;
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
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the ticket-service.
 * <p>
 * Loads the full Spring context with an in-memory H2 database and exercises
 * the complete stack from HTTP request through controller, service, mapper,
 * and JPA repository.
 * </p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application.properties")
@Transactional
@DisplayName("Ticket Service — integration tests")
class TicketIntegrationTest {

    @MockBean
    private ITicketEventPublisher ticketEventPublisher;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static CreateTicketRequest buildCreateRequest() {
        return new CreateTicketRequest("550e8400-e29b-41d4-a716-446655440000", "user-001");
    }

    @Test
    @DisplayName("should purchase a ticket and return 201 with a server-generated UUID id")
    void create_returnsCreatedWithUuid() throws Exception {
        CreateTicketRequest request = buildCreateRequest();

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.eventId").value("550e8400-e29b-41d4-a716-446655440000"))
                .andExpect(jsonPath("$.userId").value("user-001"))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("should purchase a ticket and retrieve it by the returned ID")
    void createAndGetById_success() throws Exception {
        // Purchase
        MvcResult createResult = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Get by ID
        mockMvc.perform(get("/api/v1/tickets/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.userId").value("user-001"));
    }

    @Test
    @DisplayName("should purchase a ticket, transfer it, and return the updated userId")
    void createAndUpdate_success() throws Exception {
        // Purchase
        MvcResult createResult = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Transfer
        UpdateTicketRequest updateRequest = new UpdateTicketRequest("user-002");

        mockMvc.perform(put("/api/v1/tickets/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.userId").value("user-002"));
    }

    @Test
    @DisplayName("should purchase a ticket and list it in the paginated response")
    void createAndGetAll_returnsInPage() throws Exception {
        CreateTicketRequest request = new CreateTicketRequest("aabbccdd-1234-5678-abcd-000000000001", "user-filter-test");

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/tickets").param("userId", "user-filter-test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].userId").value("user-filter-test"));
    }

    @Test
    @DisplayName("should cancel a confirmed ticket and return CANCELLED status")
    void createAndCancel_success() throws Exception {
        // Purchase
        MvcResult createResult = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Cancel
        mockMvc.perform(patch("/api/v1/tickets/{id}/cancel", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("should return 409 when cancelling an already-cancelled ticket")
    void cancelAlreadyCancelled_returns409() throws Exception {
        // Purchase
        MvcResult createResult = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // First cancel
        mockMvc.perform(patch("/api/v1/tickets/{id}/cancel", id))
                .andExpect(status().isOk());

        // Second cancel — should conflict
        mockMvc.perform(patch("/api/v1/tickets/{id}/cancel", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("should soft-delete a ticket and return 404 on subsequent retrieval")
    void createAndDelete_notFoundAfterDeletion() throws Exception {
        // Purchase
        MvcResult createResult = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Delete
        mockMvc.perform(delete("/api/v1/tickets/{id}", id))
                .andExpect(status().isNoContent());

        // Get by ID — should be 404 now
        mockMvc.perform(get("/api/v1/tickets/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("should return 404 when getting a non-existent ticket")
    void getById_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("should return 400 when creating a ticket with invalid data")
    void create_invalidRequest_returns400() throws Exception {
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
