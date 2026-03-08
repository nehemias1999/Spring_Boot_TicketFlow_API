package com.ticketflow.event_service.catalog.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for creating a new catalog entry.
 * <p>
 * All fields are validated using Jakarta Bean Validation constraints
 * to ensure data integrity before reaching the domain layer.
 * </p>
 *
 * @param id          unique business identifier (e.g., "EVT-001"), max 20 characters
 * @param title       name of the event (e.g., "Lollapalooza 2026"), between 3 and 150 characters
 * @param description short summary of the event, max 500 characters
 * @param date        date and time of the event as a string (e.g., "2026-10-15 20:00")
 * @param location    venue where the event takes place, max 200 characters
 * @param basePrice   base reference price for the event, must be >= 0 with up to 2 decimal places
 * @author TicketFlow Team
 */
public record CreateCatalogRequest(

        @NotBlank(message = "ID is required")
        @Size(max = 20, message = "ID must not exceed 20 characters")
        String id,

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotBlank(message = "Date is required")
        String date,

        @NotBlank(message = "Location is required")
        @Size(max = 200, message = "Location must not exceed 200 characters")
        String location,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Base price must be greater than or equal to 0")
        @Digits(integer = 10, fraction = 2, message = "Base price must have at most 10 integer digits and 2 decimal places")
        BigDecimal basePrice
) {
}

