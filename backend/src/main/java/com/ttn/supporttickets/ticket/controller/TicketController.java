package com.ttn.supporttickets.ticket.controller;

import com.ttn.supporttickets.ticket.dto.AddCommentRequest;
import com.ttn.supporttickets.ticket.dto.ChangeTicketStatusRequest;
import com.ttn.supporttickets.ticket.dto.CommentResponse;
import com.ttn.supporttickets.ticket.dto.CreateTicketRequest;
import com.ttn.supporttickets.ticket.dto.PageResponse;
import com.ttn.supporttickets.ticket.dto.TicketResponse;
import com.ttn.supporttickets.ticket.dto.TicketSummaryResponse;
import com.ttn.supporttickets.ticket.dto.UpdateTicketRequest;
import com.ttn.supporttickets.ticket.enums.TicketStatus;
import com.ttn.supporttickets.ticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * REST API for ticket lifecycle operations.
 */
@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + response.getId())).body(response);
    }

    @GetMapping
    public PageResponse<TicketSummaryResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort
    ) {
        TicketStatus ticketStatus = status == null || status.isBlank()
                ? null
                : TicketStatus.valueOf(status);
        return ticketService.list(q, ticketStatus, page, size, sort);
    }

    @GetMapping("/{ticketId}")
    public TicketResponse getById(@PathVariable UUID ticketId) {
        return ticketService.getById(ticketId);
    }

    @PatchMapping("/{ticketId}")
    public TicketResponse update(
            @PathVariable UUID ticketId,
            @Valid @RequestBody UpdateTicketRequest request
    ) {
        return ticketService.update(ticketId, request);
    }

    @PostMapping("/{ticketId}/status")
    public TicketResponse changeStatus(
            @PathVariable UUID ticketId,
            @Valid @RequestBody ChangeTicketStatusRequest request
    ) {
        return ticketService.changeStatus(ticketId, request);
    }

    @PostMapping("/{ticketId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable UUID ticketId,
            @Valid @RequestBody AddCommentRequest request
    ) {
        CommentResponse response = ticketService.addComment(ticketId, request);
        URI location = URI.create("/api/v1/tickets/" + ticketId + "/comments/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }
}
