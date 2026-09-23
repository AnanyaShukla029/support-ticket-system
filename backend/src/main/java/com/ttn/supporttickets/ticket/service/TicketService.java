package com.ttn.supporttickets.ticket.service;

import com.ttn.supporttickets.shared.exception.TicketNotFoundException;
import com.ttn.supporttickets.ticket.dto.AddCommentRequest;
import com.ttn.supporttickets.ticket.dto.ChangeTicketStatusRequest;
import com.ttn.supporttickets.ticket.dto.CommentResponse;
import com.ttn.supporttickets.ticket.dto.CreateTicketRequest;
import com.ttn.supporttickets.ticket.dto.PageResponse;
import com.ttn.supporttickets.ticket.dto.TicketResponse;
import com.ttn.supporttickets.ticket.dto.TicketSummaryResponse;
import com.ttn.supporttickets.ticket.dto.UpdateTicketRequest;
import com.ttn.supporttickets.ticket.entity.Comment;
import com.ttn.supporttickets.ticket.entity.Ticket;
import com.ttn.supporttickets.ticket.enums.TicketStatus;
import com.ttn.supporttickets.ticket.mapper.TicketMapper;
import com.ttn.supporttickets.ticket.repository.CommentRepository;
import com.ttn.supporttickets.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Application service for ticket lifecycle operations.
 */
@Service
@RequiredArgsConstructor
public class TicketService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "updatedAt", "createdAt", "title", "priority", "status"
    );

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;
    private final TicketMapper ticketMapper;
    private final TicketStatusMachine statusMachine;

    @Transactional
    public TicketResponse create(CreateTicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setTitle(request.getTitle().trim());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority());
        ticket.setAssignee(normalize(request.getAssignee()));
        ticket.setStatus(TicketStatus.OPEN);
        LocalDateTime now = LocalDateTime.now();
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);
        return ticketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(UUID ticketId) {
        Ticket ticket = findTicketOrThrow(ticketId);
        attachComments(ticket);
        return ticketMapper.toResponse(ticket);
    }

    @Transactional
    public TicketResponse update(UUID ticketId, UpdateTicketRequest request) {
        Ticket ticket = findTicketOrThrow(ticketId);

        if (request.getTitle() != null) {
            ticket.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            ticket.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        if (request.getAssignee() != null) {
            ticket.setAssignee(normalize(request.getAssignee()));
        }

        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);
        attachComments(saved);
        return ticketMapper.toResponse(saved);
    }

    public PageResponse<TicketSummaryResponse> list(
            String keyword,
            TicketStatus status,
            int page,
            int size,
            String sort
    ) {
        Pageable pageable = pageable(page, size, sort);
        Page<Ticket> results = ticketRepository.search(normalizeKeyword(keyword), status, pageable);
        return ticketMapper.toPage(results);
    }

    @Transactional
    public TicketResponse changeStatus(UUID ticketId, ChangeTicketStatusRequest request) {
        Ticket ticket = findTicketOrThrow(ticketId);
        statusMachine.assertTransition(ticket.getStatus(), request.getStatus());
        ticket.setStatus(request.getStatus());
        ticket.setUpdatedAt(LocalDateTime.now());
        Ticket saved = ticketRepository.save(ticket);
        attachComments(saved);
        return ticketMapper.toResponse(saved);
    }

    @Transactional
    public CommentResponse addComment(UUID ticketId, AddCommentRequest request) {
        Ticket ticket = findTicketOrThrow(ticketId);
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setBody(request.getBody().trim());
        comment.setCreatedAt(LocalDateTime.now());
        return ticketMapper.toCommentResponse(commentRepository.save(comment));
    }

    private Ticket findTicketOrThrow(UUID ticketId) {
        return ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
    }

    private void attachComments(Ticket ticket) {
        List<Comment> comments = commentRepository.findByTicket_IdOrderByCreatedAtAsc(ticket.getId());
        ticket.getComments().clear();
        ticket.getComments().addAll(comments);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private Pageable pageable(int page, int size, String sort) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be greater than or equal to 0");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("size must be between 1 and 100");
        }
        String sortValue = sort == null || sort.isBlank() ? "updatedAt,desc" : sort.trim();
        String[] parts = sortValue.split(",", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("sort must be property,direction");
        }

        String property = parts[0].trim();
        String direction = parts[1].trim();
        if (!ALLOWED_SORT_PROPERTIES.contains(property)) {
            throw new IllegalArgumentException("unsupported sort property: " + property);
        }

        Sort.Direction sortDirection = Sort.Direction.fromOptionalString(direction)
                .orElseThrow(() -> new IllegalArgumentException("unsupported sort direction: " + direction));

        return PageRequest.of(page, size, Sort.by(sortDirection, property));
    }
}
