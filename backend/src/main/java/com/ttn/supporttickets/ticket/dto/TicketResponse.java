package com.ttn.supporttickets.ticket.dto;

import com.ttn.supporttickets.ticket.enums.Priority;
import com.ttn.supporttickets.ticket.enums.TicketStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * API representation of a ticket with details and comments.
 */
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {

    private UUID id;
    private String title;
    private TicketStatus status;
    private Priority priority;
    private String assignee;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String description;
    private List<CommentResponse> comments;
}
