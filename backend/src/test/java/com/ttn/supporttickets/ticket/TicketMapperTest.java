package com.ttn.supporttickets.ticket;

import com.ttn.supporttickets.ticket.dto.CommentResponse;
import com.ttn.supporttickets.ticket.dto.TicketResponse;
import com.ttn.supporttickets.ticket.dto.TicketSummaryResponse;
import com.ttn.supporttickets.ticket.entity.Comment;
import com.ttn.supporttickets.ticket.entity.Ticket;
import com.ttn.supporttickets.ticket.enums.Priority;
import com.ttn.supporttickets.ticket.enums.TicketStatus;
import com.ttn.supporttickets.ticket.mapper.TicketMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TicketMapperTest {

    private final TicketMapper mapper = new TicketMapper();

    @Test
    void shouldMapTicketToSummary() {
        Ticket ticket = TicketFixtures.openTicket();
        ticket.setAssignee("Alex");
        ticket.setCreatedAt(LocalDateTime.parse("2026-09-21T10:15:30"));
        ticket.setUpdatedAt(LocalDateTime.parse("2026-09-21T10:16:00"));

        TicketSummaryResponse summary = mapper.toSummary(ticket);

        assertThat(summary.getId()).isEqualTo(ticket.getId());
        assertThat(summary.getTitle()).isEqualTo("Cannot login");
        assertThat(summary.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(summary.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(summary.getAssignee()).isEqualTo("Alex");
        assertThat(summary.getCreatedAt()).isEqualTo(LocalDateTime.parse("2026-09-21T10:15:30"));
        assertThat(summary.getUpdatedAt()).isEqualTo(LocalDateTime.parse("2026-09-21T10:16:00"));
    }

    @Test
    void shouldMapTicketToResponseWithCommentsOldestFirst() {
        Ticket ticket = TicketFixtures.openTicket();
        Comment first = CommentFixtures.comment(ticket, "First");
        Comment second = CommentFixtures.comment(ticket, "Second");
        ticket.getComments().addAll(List.of(first, second));

        TicketResponse response = mapper.toResponse(ticket);

        assertThat(response.getDescription()).isEqualTo("Credentials rejected");
        assertThat(response.getComments()).containsExactly(
                new CommentResponse(first.getId(), "First", first.getCreatedAt()),
                new CommentResponse(second.getId(), "Second", second.getCreatedAt())
        );
    }

    @Test
    void shouldReturnEmptyCommentsWhenTicketHasNoComments() {
        Ticket ticket = TicketFixtures.openTicket();

        TicketResponse response = mapper.toResponse(ticket);

        assertThat(response.getComments()).isEmpty();
    }

    @Test
    void shouldMapPageOfTickets() {
        Ticket ticket = TicketFixtures.openTicket();
        Page<Ticket> page = new PageImpl<>(List.of(ticket), PageRequest.of(0, 20), 1);

        var response = mapper.toPage(page);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(20);
        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
    }
}
