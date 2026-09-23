package com.ttn.supporttickets.ticket.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * API representation of a ticket comment.
 */
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private UUID id;
    private String body;
    private LocalDateTime createdAt;
}
