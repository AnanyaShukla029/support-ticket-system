package com.ttn.supporttickets.ticket.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for adding a comment to a ticket.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddCommentRequest {

    @NotBlank
    @Size(max = 4000)
    private String body;
}
