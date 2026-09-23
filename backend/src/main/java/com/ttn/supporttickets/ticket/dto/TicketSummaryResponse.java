package com.ttn.supporttickets.ticket.dto;

import com.ttn.supporttickets.ticket.enums.Priority;
import com.ttn.supporttickets.ticket.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * API representation of a ticket in list responses.
 */
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TicketSummaryResponse {

    private UUID id;
    private String title;
    private TicketStatus status;
    private Priority priority;
    private String assignee;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
