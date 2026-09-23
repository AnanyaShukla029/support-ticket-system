package com.ttn.supporttickets.ticket;

import com.ttn.supporttickets.shared.exception.IllegalTicketTransitionException;
import com.ttn.supporttickets.ticket.enums.TicketStatus;
import com.ttn.supporttickets.ticket.service.TicketStatusMachine;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketStatusMachineTest {

    private final TicketStatusMachine machine = new TicketStatusMachine();

    @ParameterizedTest
    @MethodSource("allowedTransitions")
    void shouldAllowValidTransitions(TicketStatus from, TicketStatus to) {
        assertThat(machine.isAllowed(from, to)).isTrue();
        assertThatCode(() -> machine.assertTransition(from, to)).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @MethodSource("rejectedTransitions")
    void shouldRejectInvalidTransitions(TicketStatus from, TicketStatus to) {
        assertThat(machine.isAllowed(from, to)).isFalse();
        assertThatThrownBy(() -> machine.assertTransition(from, to))
                .isInstanceOf(IllegalTicketTransitionException.class)
                .hasMessage("Ticket in status " + from.name() + " cannot move to " + to.name());
    }

    private static Stream<Arguments> allowedTransitions() {
        return Stream.of(
                Arguments.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.OPEN, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.CLOSED)
        );
    }

    private static Stream<Arguments> rejectedTransitions() {
        return Stream.of(
                Arguments.of(TicketStatus.OPEN, TicketStatus.OPEN),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.CLOSED, TicketStatus.CLOSED),
                Arguments.of(TicketStatus.CANCELLED, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.OPEN, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.OPEN, TicketStatus.CLOSED),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.OPEN),
                Arguments.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.OPEN),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.CLOSED, TicketStatus.OPEN),
                Arguments.of(TicketStatus.CLOSED, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.CLOSED, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.CLOSED, TicketStatus.CANCELLED),
                Arguments.of(TicketStatus.CANCELLED, TicketStatus.OPEN),
                Arguments.of(TicketStatus.CANCELLED, TicketStatus.IN_PROGRESS),
                Arguments.of(TicketStatus.CANCELLED, TicketStatus.RESOLVED),
                Arguments.of(TicketStatus.CANCELLED, TicketStatus.CLOSED)
        );
    }
}
