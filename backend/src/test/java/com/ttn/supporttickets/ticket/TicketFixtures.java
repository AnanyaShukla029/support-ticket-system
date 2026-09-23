package com.ttn.supporttickets.ticket;

import com.ttn.supporttickets.ticket.entity.Ticket;
import com.ttn.supporttickets.ticket.enums.Priority;
import com.ttn.supporttickets.ticket.enums.TicketStatus;

import java.util.UUID;

/**
 * Test builders for {@link Ticket} entities.
 */
public final class TicketFixtures {

    private TicketFixtures() {
    }

    public static Ticket openTicket() {
        return ticket("Cannot login", "Credentials rejected", Priority.HIGH, TicketStatus.OPEN);
    }

    public static Ticket ticket(String title, String description, Priority priority, TicketStatus status) {
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setPriority(priority);
        ticket.setStatus(status);
        return ticket;
    }
}
