package com.ttn.supporttickets.ticket;

import com.ttn.supporttickets.ticket.entity.Comment;
import com.ttn.supporttickets.ticket.entity.Ticket;
import com.ttn.supporttickets.ticket.enums.Priority;
import com.ttn.supporttickets.ticket.enums.TicketStatus;
import com.ttn.supporttickets.ticket.repository.CommentRepository;
import com.ttn.supporttickets.ticket.repository.TicketRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@Tag("integration")
@Transactional
class TicketRepositoryIT {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void shouldPersistTicketAndComment() {
        Ticket ticket = newTicket("Cannot login", "Credentials rejected", Priority.HIGH);
        ticket = ticketRepository.saveAndFlush(ticket);

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setBody("Checking auth logs.");
        comment = commentRepository.saveAndFlush(comment);

        Ticket loaded = ticketRepository.findById(ticket.getId()).orElseThrow();
        Comment loadedComment = commentRepository.findById(comment.getId()).orElseThrow();

        assertThat(loaded.getTitle()).isEqualTo("Cannot login");
        assertThat(loaded.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(loadedComment.getBody()).isEqualTo("Checking auth logs.");
        assertThat(loadedComment.getTicket().getId()).isEqualTo(ticket.getId());
    }

    @Test
    void shouldSearchByKeywordCaseInsensitively() {
        ticketRepository.saveAndFlush(newTicket("Password reset", "User locked out", Priority.MEDIUM));
        ticketRepository.saveAndFlush(newTicket("Billing issue", "Invoice mismatch", Priority.LOW));

        Page<Ticket> titleMatch = ticketRepository.search("password", null, PageRequest.of(0, 20));
        Page<Ticket> descriptionMatch = ticketRepository.search("invoice", null, PageRequest.of(0, 20));

        assertThat(titleMatch.getContent()).hasSize(1);
        assertThat(titleMatch.getContent().getFirst().getTitle()).isEqualTo("Password reset");
        assertThat(descriptionMatch.getContent()).hasSize(1);
        assertThat(descriptionMatch.getContent().getFirst().getTitle()).isEqualTo("Billing issue");
    }

    @Test
    void shouldFilterByStatus() {
        Ticket open = newTicket("Open ticket", "Still open", Priority.LOW);
        Ticket inProgress = newTicket("Working ticket", "In progress", Priority.MEDIUM);
        inProgress.setStatus(TicketStatus.IN_PROGRESS);
        ticketRepository.saveAllAndFlush(List.of(open, inProgress));

        Page<Ticket> openTickets = ticketRepository.search(null, TicketStatus.OPEN, PageRequest.of(0, 20));

        assertThat(openTickets.getContent()).hasSize(1);
        assertThat(openTickets.getContent().getFirst().getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void shouldPaginateAndSortByUpdatedAtDescending() throws InterruptedException {
        Ticket older = newTicket("Older ticket", "Created first", Priority.LOW);
        ticketRepository.saveAndFlush(older);

        Thread.sleep(5);

        Ticket newer = newTicket("Newer ticket", "Created second", Priority.HIGH);
        ticketRepository.saveAndFlush(newer);

        Page<Ticket> page = ticketRepository.search(
                null,
                null,
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );

        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getTitle()).isEqualTo("Newer ticket");
    }

    @Test
    void shouldReturnCommentsOldestFirst() throws InterruptedException {
        Ticket ticket = ticketRepository.saveAndFlush(newTicket("Needs triage", "Please review", Priority.MEDIUM));

        Comment first = new Comment();
        first.setTicket(ticket);
        first.setBody("First comment");
        commentRepository.saveAndFlush(first);

        Thread.sleep(5);

        Comment second = new Comment();
        second.setTicket(ticket);
        second.setBody("Second comment");
        commentRepository.saveAndFlush(second);

        List<Comment> comments = commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId());

        assertThat(comments).hasSize(2);
        assertThat(comments.get(0).getBody()).isEqualTo("First comment");
        assertThat(comments.get(1).getBody()).isEqualTo("Second comment");
    }

    @Test
    void shouldNotChangeTicketUpdatedAtWhenOnlyCommentIsAdded() throws InterruptedException {
        Ticket ticket = ticketRepository.saveAndFlush(newTicket("Stable ticket", "No field edits", Priority.LOW));
        LocalDateTime updatedAtBeforeComment = ticket.getUpdatedAt();

        Thread.sleep(5);

        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setBody("Follow-up note");
        commentRepository.saveAndFlush(comment);

        Ticket reloaded = ticketRepository.findById(ticket.getId()).orElseThrow();

        assertThat(reloaded.getUpdatedAt()).isEqualTo(updatedAtBeforeComment);
    }

    private Ticket newTicket(String title, String description, Priority priority) {
        Ticket ticket = new Ticket();
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setPriority(priority);
        return ticket;
    }
}
