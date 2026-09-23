package com.ttn.supporttickets.ticket.dto;

import com.ttn.supporttickets.ticket.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for creating a ticket.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateTicketRequest {

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotBlank
    @Size(max = 10000)
    private String description;

    @NotNull
    private Priority priority;

    @Size(max = 120)
    private String assignee;
}
