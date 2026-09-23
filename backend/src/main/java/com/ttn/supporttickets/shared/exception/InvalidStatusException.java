package com.ttn.supporttickets.shared.exception;

/**
 * Thrown when a request contains a status value that is not a valid {@code TicketStatus}.
 */
public class InvalidStatusException extends RuntimeException {

    public InvalidStatusException() {
        super("Invalid ticket status value");
    }
}
