package com.ttn.supporttickets.ticket.dto;

import com.ttn.supporttickets.ticket.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for changing a ticket's status.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeTicketStatusRequest {

    @NotNull
    private TicketStatus status;
}
