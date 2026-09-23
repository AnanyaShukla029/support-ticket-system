package com.ttn.supporttickets.shared.exception;

/**
 * Thrown when a requested ticket status transition is not allowed by the state machine.
 */
public class IllegalTicketTransitionException extends RuntimeException {

    private final String currentStatus;
    private final String requestedStatus;

    public IllegalTicketTransitionException(String currentStatus, String requestedStatus) {
        super("Ticket in status " + currentStatus + " cannot move to " + requestedStatus);
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getRequestedStatus() {
        return requestedStatus;
    }
}
