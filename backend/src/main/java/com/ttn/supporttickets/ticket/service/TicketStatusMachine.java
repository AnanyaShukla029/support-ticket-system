package com.ttn.supporttickets.ticket.service;

import com.ttn.supporttickets.shared.exception.IllegalTicketTransitionException;
import com.ttn.supporttickets.ticket.enums.TicketStatus;
import org.springframework.stereotype.Component;

/**
 * Encodes allowed ticket status transitions from {@code spec/state-machine.md}.
 */
@Component
public class TicketStatusMachine {

    /**
     * Returns whether {@code from} may move to {@code to}.
     */
    public boolean isAllowed(TicketStatus from, TicketStatus to) {
        if (from == to) {
            return false;
        }
        return switch (from) {
            case OPEN -> to == TicketStatus.IN_PROGRESS || to == TicketStatus.CANCELLED;
            case IN_PROGRESS -> to == TicketStatus.RESOLVED || to == TicketStatus.CANCELLED;
            case RESOLVED -> to == TicketStatus.CLOSED;
            case CLOSED, CANCELLED -> false;
        };
    }

    /**
     * Validates the transition or throws {@link IllegalTicketTransitionException}.
     */
    public void assertTransition(TicketStatus from, TicketStatus to) {
        if (!isAllowed(from, to)) {
            throw new IllegalTicketTransitionException(from.name(), to.name());
        }
    }
}
