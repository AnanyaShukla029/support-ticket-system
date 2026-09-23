package com.ttn.supporttickets.shared.exception.handler;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.ttn.supporttickets.shared.exception.ApiError;
import com.ttn.supporttickets.shared.exception.ErrorDetail;
import com.ttn.supporttickets.shared.exception.IllegalTicketTransitionException;
import com.ttn.supporttickets.shared.exception.InvalidStatusException;
import com.ttn.supporttickets.shared.exception.TicketNotFoundException;
import com.ttn.supporttickets.ticket.enums.Priority;
import com.ttn.supporttickets.ticket.enums.TicketStatus;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.UUID;

/**
 * Maps domain and framework exceptions to the shared {@link ApiError} shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        List<ErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
                .map(this::toErrorDetail)
                .toList();
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleBadRequest(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof InvalidFormatException invalidFormat) {
            if (invalidFormat.getTargetType() == Priority.class) {
                return error(HttpStatus.BAD_REQUEST, "INVALID_PRIORITY", "Invalid priority value", List.of());
            }
            if (invalidFormat.getTargetType() == TicketStatus.class) {
                return error(HttpStatus.BAD_REQUEST, "INVALID_STATUS", "Invalid ticket status value", List.of());
            }
        }
        List<ErrorDetail> details = extractMessageNotReadableDetails(exception);
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", details);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return error(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "UNSUPPORTED_MEDIA_TYPE",
                "Content-Type must be application/json",
                List.of()
        );
    }

    @ExceptionHandler(TicketNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(TicketNotFoundException exception) {
        return error(HttpStatus.NOT_FOUND, "TICKET_NOT_FOUND", exception.getMessage(), List.of());
    }

    @ExceptionHandler(IllegalTicketTransitionException.class)
    ResponseEntity<ApiError> handleIllegalTransition(IllegalTicketTransitionException exception) {
        return error(
                HttpStatus.CONFLICT,
                "ILLEGAL_TICKET_TRANSITION",
                exception.getMessage(),
                List.of()
        );
    }

    @ExceptionHandler(InvalidStatusException.class)
    ResponseEntity<ApiError> handleInvalidStatus(InvalidStatusException exception) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_STATUS", exception.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnhandled(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                List.of()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        if (exception.getRequiredType() == UUID.class) {
            return error(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_FAILED",
                    "Request validation failed",
                    List.of(new ErrorDetail("ticketId", "must be a valid UUID"))
            );
        }
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed", List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage();
        if (message.contains("page") || message.contains("size")) {
            return error(HttpStatus.BAD_REQUEST, "INVALID_PAGE", message, List.of());
        }
        if (message.contains("sort")) {
            return error(HttpStatus.BAD_REQUEST, "INVALID_SORT", message, List.of());
        }
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message, List.of());
    }

    /*
     * Keep the response construction in one place so every exception has the
     * same error shape and correlation-id behavior.
     */
    private ResponseEntity<ApiError> error(
            HttpStatus status,
            String code,
            String message,
            List<ErrorDetail> details
    ) {
        return ResponseEntity.status(status)
                .body(new ApiError(code, message, details));
    }

    private ErrorDetail toErrorDetail(FieldError fieldError) {
        return new ErrorDetail(fieldError.getField(), fieldError.getDefaultMessage());
    }

    private List<ErrorDetail> extractMessageNotReadableDetails(HttpMessageNotReadableException exception) {
        Throwable cause = exception.getCause();
        if (cause instanceof UnrecognizedPropertyException unrecognizedProperty) {
            String message = "status".equals(unrecognizedProperty.getPropertyName())
                    ? "use POST /api/v1/tickets/{ticketId}/status"
                    : "is not recognized";
            return List.of(new ErrorDetail(unrecognizedProperty.getPropertyName(), message));
        }
        return List.of();
    }
}
