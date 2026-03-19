package com.ticketflow.event_service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketflow.event_service.catalog.application.dto.request.CreateEventRequest;
import com.ticketflow.event_service.catalog.application.dto.request.UpdateEventRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the event-service.
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
@DisplayName("Event Service — integration tests")
class EventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static CreateEventRequest buildCreateRequest() {
        return new CreateEventRequest(
                "Lollapalooza 2026",
                "Annual music festival",
                "2026-10-15 20:00",
                "Estadio River Plate",
                BigDecimal.valueOf(150.00)
        );
    }

    @Test
    @DisplayName("should create an event and return 201 with a server-generated UUID id")
    void create_returnsCreatedWithUuid() throws Exception {
        CreateEventRequest request = buildCreateRequest();

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.title").value("Lollapalooza 2026"))
                .andExpect(jsonPath("$.location").value("Estadio River Plate"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    @DisplayName("should create an event and retrieve it by the returned ID")
    void createAndGetById_success() throws Exception {
        // Create
        MvcResult createResult = mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String id = objectMapper.readTree(responseBody).get("id").asText();

        // Get by ID
        mockMvc.perform(get("/api/v1/events/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Lollapalooza 2026"));
    }

    @Test
    @DisplayName("should create an event, update it, and return the updated data")
    void createAndUpdate_success() throws Exception {
        // Create
        MvcResult createResult = mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Update
        UpdateEventRequest updateRequest = new UpdateEventRequest(
                "Lollapalooza 2026 Updated",
                "Updated description",
                "2026-10-20 20:00",
                "Estadio Monumental",
                BigDecimal.valueOf(200.00)
        );

        mockMvc.perform(put("/api/v1/events/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.title").value("Lollapalooza 2026 Updated"))
                .andExpect(jsonPath("$.location").value("Estadio Monumental"));
    }

    @Test
    @DisplayName("should create an event and list it in the paginated response")
    void createAndGetAll_returnsInPage() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/events").param("title", "Lollapalooza"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Lollapalooza 2026"));
    }

    @Test
    @DisplayName("should soft-delete an event and return 404 on subsequent retrieval")
    void createAndDelete_notFoundAfterDeletion() throws Exception {
        // Create
        MvcResult createResult = mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest())))
                .andExpect(status().isCreated())
                .andReturn();

        String id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();

        // Delete
        mockMvc.perform(delete("/api/v1/events/{id}", id))
                .andExpect(status().isNoContent());

        // Get by ID — should be 404 now
        mockMvc.perform(get("/api/v1/events/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("should return 404 when getting a non-existent event")
    void getById_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/v1/events/non-existent-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("should return 400 when creating an event with invalid data")
    void create_invalidRequest_returns400() throws Exception {
        String invalidBody = """
                {
                  "title": "a",
                  "description": "",
                  "date": "",
                  "location": "",
                  "basePrice": -1
                }
                """;

        mockMvc.perform(post("/api/v1/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
