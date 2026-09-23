package com.ttn.supporttickets.shared.exception;

import java.util.UUID;

/**
 * Thrown when a ticket identifier does not exist.
 */
public class TicketNotFoundException extends RuntimeException {

    private final UUID ticketId;

    public TicketNotFoundException(UUID ticketId) {
        super("Ticket " + ticketId + " was not found");
        this.ticketId = ticketId;
    }

    public UUID getTicketId() {
        return ticketId;
    }
}
