package com.ttn.supporttickets.shared.exception;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Field-level validation detail within an {@link ApiError}.
 */
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDetail {

    private String field;
    private String message;
}
