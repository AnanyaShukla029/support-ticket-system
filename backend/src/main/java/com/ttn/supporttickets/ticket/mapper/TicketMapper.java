package com.ttn.supporttickets.ticket.mapper;

import com.ttn.supporttickets.ticket.dto.CommentResponse;
import com.ttn.supporttickets.ticket.dto.PageResponse;
import com.ttn.supporttickets.ticket.dto.TicketResponse;
import com.ttn.supporttickets.ticket.dto.TicketSummaryResponse;
import com.ttn.supporttickets.ticket.entity.Comment;
import com.ttn.supporttickets.ticket.entity.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Maps ticket entities to API DTOs.
 */
@Component
public class TicketMapper {

    public TicketSummaryResponse toSummary(Ticket ticket) {
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    public TicketResponse toResponse(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getDescription(),
                toCommentResponses(ticket.getComments())
        );
    }

    public CommentResponse toCommentResponse(Comment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getBody(),
                comment.getCreatedAt()
        );
    }

    public PageResponse<TicketSummaryResponse> toPage(Page<Ticket> page) {
        List<TicketSummaryResponse> items = page.getContent().stream()
                .map(this::toSummary)
                .toList();
        return new PageResponse<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private List<CommentResponse> toCommentResponses(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return List.of();
        }
        return comments.stream()
                .map(this::toCommentResponse)
                .toList();
    }
}
