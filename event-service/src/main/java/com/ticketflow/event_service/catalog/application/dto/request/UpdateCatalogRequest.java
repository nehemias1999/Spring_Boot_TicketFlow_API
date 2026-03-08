package com.ticketflow.event_service.catalog.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for updating an existing catalog entry.
 * <p>
 * Contains all mutable fields of a catalog. The ID cannot be changed
 * and is provided as a path variable in the REST endpoint.
 * </p>
 *
 * @param title       updated name of the event, between 3 and 150 characters
 * @param description updated summary of the event, max 500 characters
 * @param date        updated date and time string (e.g., "2026-10-15 20:00")
 * @param location    updated venue, max 200 characters
 * @param basePrice   updated base reference price, must be >= 0 with up to 2 decimal places
 * @author TicketFlow Team
 */
public record UpdateCatalogRequest(

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

