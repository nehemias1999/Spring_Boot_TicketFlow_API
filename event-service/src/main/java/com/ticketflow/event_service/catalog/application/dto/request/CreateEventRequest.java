package com.ticketflow.event_service.catalog.application.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Request DTO for creating a new event entry.
 *
 * @param title       name of the event, between 3 and 150 characters
 * @param description short summary of the event, max 500 characters
 * @param date        date and time of the event (must be in the future)
 * @param location    venue where the event takes place, max 200 characters
 * @param basePrice   base reference price for the event, must be >= 0
 * @param capacity    total number of available tickets (must be >= 1)
 * @author TicketFlow Team
 */
public record CreateEventRequest(

        @NotBlank(message = "Title is required")
        @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @NotNull(message = "Date is required")
        @Future(message = "Event date must be in the future")
        LocalDateTime date,

        @NotBlank(message = "Location is required")
        @Size(max = 200, message = "Location must not exceed 200 characters")
        String location,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Base price must be greater than or equal to 0")
        @Digits(integer = 10, fraction = 2, message = "Base price must have at most 10 integer digits and 2 decimal places")
        BigDecimal basePrice,

        @NotNull(message = "Capacity is required")
        @Min(value = 1, message = "Capacity must be at least 1")
        Integer capacity
) {
}
