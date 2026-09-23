package com.ttn.supporttickets.shared.exception;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Standard error body for all API 4xx/5xx responses.
 */
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class ApiError {

    private String code;
    private String message;
    private List<ErrorDetail> details;
}
