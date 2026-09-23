package com.ttn.supporttickets.ticket;

import com.ttn.supporttickets.shared.exception.IllegalTicketTransitionException;
import com.ttn.supporttickets.shared.exception.TicketNotFoundException;
import com.ttn.supporttickets.ticket.dto.AddCommentRequest;
import com.ttn.supporttickets.ticket.dto.ChangeTicketStatusRequest;
import com.ttn.supporttickets.ticket.dto.CreateTicketRequest;
import com.ttn.supporttickets.ticket.dto.PageResponse;
import com.ttn.supporttickets.ticket.dto.TicketResponse;
import com.ttn.supporttickets.ticket.dto.TicketSummaryResponse;
import com.ttn.supporttickets.ticket.dto.UpdateTicketRequest;
import com.ttn.supporttickets.ticket.entity.Comment;
import com.ttn.supporttickets.ticket.entity.Ticket;
import com.ttn.supporttickets.ticket.enums.Priority;
import com.ttn.supporttickets.ticket.enums.TicketStatus;
import com.ttn.supporttickets.ticket.mapper.TicketMapper;
import com.ttn.supporttickets.ticket.repository.CommentRepository;
import com.ttn.supporttickets.ticket.repository.TicketRepository;
import com.ttn.supporttickets.ticket.service.TicketService;
import com.ttn.supporttickets.ticket.service.TicketStatusMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    private static final LocalDateTime FIXED_TIME = LocalDateTime.parse("2026-09-21T10:00:00");
    private static final LocalDateTime FIXED_INSTANT = LocalDateTime.parse("2026-09-21T10:00:00");

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketStatusMachine statusMachine;

    private TicketService ticketService;

    @BeforeEach
    void setUp() {
        ticketService = new TicketService(ticketRepository, commentRepository, ticketMapper, statusMachine);
    }

    @Test
    void shouldCreateTicketWithOpenStatusAndNormalizedFields() {
        CreateTicketRequest request = new CreateTicketRequest(
                "  Cannot login  ",
                "Credentials rejected",
                Priority.HIGH,
                "  "
        );
        Ticket saved = TicketFixtures.openTicket();
        TicketResponse response = sampleResponse(saved);

        when(ticketRepository.save(any(Ticket.class))).thenReturn(saved);
        when(ticketMapper.toResponse(saved)).thenReturn(response);

        TicketResponse result = ticketService.create(request);

        ArgumentCaptor<Ticket> captor = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(captor.capture());

        Ticket toSave = captor.getValue();
        assertThat(toSave.getTitle()).isEqualTo("Cannot login");
        assertThat(toSave.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(toSave.getAssignee()).isNull();
        assertThat(toSave.getCreatedAt()).isEqualTo(FIXED_TIME);
        assertThat(toSave.getUpdatedAt()).isEqualTo(FIXED_TIME);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldReturnTicketWithCommentsWhenFound() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = TicketFixtures.openTicket();
        ticket.setId(ticketId);
        Comment comment = CommentFixtures.comment(ticket, "Checking logs");
        TicketResponse response = sampleResponse(ticket);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId)).thenReturn(List.of(comment));
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        TicketResponse result = ticketService.getById(ticketId);

        assertThat(ticket.getComments()).containsExactly(comment);
        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldThrowWhenTicketNotFound() {
        UUID ticketId = UUID.randomUUID();
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.getById(ticketId))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void shouldPartiallyUpdateTicketFieldsAndBumpUpdatedAt() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = TicketFixtures.openTicket();
        ticket.setId(ticketId);
        ticket.setUpdatedAt(LocalDateTime.parse("2026-09-21T09:00:00"));
        UpdateTicketRequest request = new UpdateTicketRequest(
                "Updated title",
                null,
                Priority.LOW,
                ""
        );
        TicketResponse response = sampleResponse(ticket);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId)).thenReturn(List.of());
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        ticketService.update(ticketId, request);

        assertThat(ticket.getTitle()).isEqualTo("Updated title");
        assertThat(ticket.getDescription()).isEqualTo("Credentials rejected");
        assertThat(ticket.getPriority()).isEqualTo(Priority.LOW);
        assertThat(ticket.getAssignee()).isNull();
        assertThat(ticket.getUpdatedAt()).isEqualTo(FIXED_TIME);
    }

    @Test
    void shouldSearchTicketsWithNormalizedKeywordAndPagination() {
        Ticket ticket = TicketFixtures.openTicket();
        Page<Ticket> page = new PageImpl<>(List.of(ticket), PageRequest.of(0, 20), 1);
        PageResponse<TicketSummaryResponse> response = new PageResponse<>(List.of(), 0, 20, 1, 1);

        when(ticketRepository.search(eq("login"), eq(TicketStatus.OPEN), any(Pageable.class))).thenReturn(page);
        when(ticketMapper.toPage(page)).thenReturn(response);

        PageResponse<TicketSummaryResponse> result = ticketService.list(
                "  login  ",
                TicketStatus.OPEN,
                0,
                20,
                "updatedAt,desc"
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(ticketRepository).search(eq("login"), eq(TicketStatus.OPEN), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "updatedAt"));
        assertThat(result).isEqualTo(response);
    }

    @Test
    void shouldChangeStatusWhenTransitionIsAllowed() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = TicketFixtures.openTicket();
        ticket.setId(ticketId);
        ticket.setUpdatedAt(LocalDateTime.parse("2026-09-21T09:00:00"));
        ChangeTicketStatusRequest request = new ChangeTicketStatusRequest(TicketStatus.IN_PROGRESS);
        TicketResponse response = sampleResponse(ticket);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticketId)).thenReturn(List.of());
        when(ticketRepository.save(ticket)).thenReturn(ticket);
        when(ticketMapper.toResponse(ticket)).thenReturn(response);

        ticketService.changeStatus(ticketId, request);

        verify(statusMachine).assertTransition(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getUpdatedAt()).isEqualTo(FIXED_TIME);
    }

    @Test
    void shouldRejectIllegalStatusTransition() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = TicketFixtures.ticket("Closed", "Done", Priority.LOW, TicketStatus.CLOSED);
        ticket.setId(ticketId);
        ChangeTicketStatusRequest request = new ChangeTicketStatusRequest(TicketStatus.OPEN);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        org.mockito.Mockito.doThrow(new IllegalTicketTransitionException("CLOSED", "OPEN"))
                .when(statusMachine)
                .assertTransition(TicketStatus.CLOSED, TicketStatus.OPEN);

        assertThatThrownBy(() -> ticketService.changeStatus(ticketId, request))
                .isInstanceOf(IllegalTicketTransitionException.class);

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void shouldAddCommentWithoutUpdatingTicket() {
        UUID ticketId = UUID.randomUUID();
        Ticket ticket = TicketFixtures.openTicket();
        ticket.setId(ticketId);
        ticket.setUpdatedAt(LocalDateTime.parse("2026-09-21T09:00:00"));
        Comment savedComment = CommentFixtures.comment(ticket, "Follow-up");

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(ticketMapper.toCommentResponse(savedComment)).thenReturn(
                new com.ttn.supporttickets.ticket.dto.CommentResponse(
                        savedComment.getId(),
                        "Follow-up",
                        savedComment.getCreatedAt()
                )
        );

        ticketService.addComment(ticketId, new AddCommentRequest("Follow-up"));

        verify(commentRepository).save(any(Comment.class));
        verify(ticketRepository, never()).save(any());
        assertThat(ticket.getUpdatedAt()).isEqualTo(LocalDateTime.parse("2026-09-21T09:00:00"));
    }

    private TicketResponse sampleResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                FIXED_INSTANT,
                FIXED_INSTANT,
                ticket.getDescription(),
                List.of()
        );
    }
}
