package com.ttn.supporttickets.ticket.controller;

import com.ttn.supporttickets.shared.exception.IllegalTicketTransitionException;
import com.ttn.supporttickets.shared.exception.TicketNotFoundException;
import com.ttn.supporttickets.shared.exception.handler.GlobalExceptionHandler;
import com.ttn.supporttickets.ticket.dto.CommentResponse;
import com.ttn.supporttickets.ticket.dto.PageResponse;
import com.ttn.supporttickets.ticket.dto.TicketResponse;
import com.ttn.supporttickets.ticket.dto.TicketSummaryResponse;
import com.ttn.supporttickets.ticket.enums.Priority;
import com.ttn.supporttickets.ticket.enums.TicketStatus;
import com.ttn.supporttickets.ticket.service.TicketService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TicketController.class)
@Import(GlobalExceptionHandler.class)
class TicketControllerTest {

    private static final LocalDateTime NOW = LocalDateTime.now(ZoneId.of("2026-09-21T10:15:30Z"));
    private static final UUID TICKET_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    private static final UUID COMMENT_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    @Test
    void shouldCreateTicketWithLocationHeader() throws Exception {
        UUID ticketId = TICKET_ID;
        TicketResponse response = ticketResponse(ticketId);
        when(ticketService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Cannot reset password",
                                  "description": "Reset form returns 500.",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/tickets/" + ticketId))
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void shouldReturnValidationFailedWhenTitleIsBlankOnCreate() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "   ",
                                  "description": "Reset form returns 500.",
                                  "priority": "HIGH"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("title"));
    }

    @Test
    void shouldReturnInvalidPriorityWhenPriorityIsUnknownOnCreate() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Cannot reset password",
                                  "description": "Reset form returns 500.",
                                  "priority": "URGENT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PRIORITY"));
    }

    @Test
    void shouldListTicketsWithPaginationEnvelope() throws Exception {
        UUID ticketId = TICKET_ID;
        PageResponse<TicketSummaryResponse> page = new PageResponse<>(
                List.of(new TicketSummaryResponse(
                        ticketId,
                        "Cannot reset password",
                        TicketStatus.OPEN,
                        Priority.HIGH,
                        null,
                        NOW,
                        NOW
                )),
                0,
                20,
                1,
                1
        );
        when(ticketService.list(isNull(), isNull(), eq(0), eq(20), isNull())).thenReturn(page);

        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(ticketId.toString()))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void shouldReturnInvalidStatusWhenStatusQueryParamIsUnknown() throws Exception {
        mockMvc.perform(get("/api/v1/tickets").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS"));
    }

    @Test
    void shouldReturnInvalidPageWhenPageIsNegative() throws Exception {
        when(ticketService.list(isNull(), isNull(), eq(-1), eq(20), isNull()))
                .thenThrow(new IllegalArgumentException("page must be greater than or equal to 0"));

        mockMvc.perform(get("/api/v1/tickets").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PAGE"));
    }

    @Test
    void shouldReturnTicketById() throws Exception {
        UUID ticketId = TICKET_ID;
        when(ticketService.getById(ticketId)).thenReturn(ticketResponse(ticketId));

        mockMvc.perform(get("/api/v1/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.comments").isArray());
    }

    @Test
    void shouldReturnNotFoundWhenTicketDoesNotExist() throws Exception {
        UUID ticketId = TICKET_ID;
        when(ticketService.getById(ticketId)).thenThrow(new TicketNotFoundException(ticketId));

        mockMvc.perform(get("/api/v1/tickets/{ticketId}", ticketId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }

    @Test
    void shouldReturnValidationFailedWhenTicketIdIsNotUuid() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("ticketId"));
    }

    @Test
    void shouldUpdateTicketFields() throws Exception {
        UUID ticketId = TICKET_ID;
        when(ticketService.update(eq(ticketId), any())).thenReturn(ticketResponse(ticketId));

        mockMvc.perform(patch("/api/v1/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Cannot reset password (prod)"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()));
    }

    @Test
    void shouldRejectStatusFieldOnPatch() throws Exception {
        UUID ticketId = TICKET_ID;

        mockMvc.perform(patch("/api/v1/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("status"))
                .andExpect(jsonPath("$.details[0].message")
                        .value("use POST /api/v1/tickets/{ticketId}/status"));
    }

    @Test
    void shouldChangeStatus() throws Exception {
        UUID ticketId = TICKET_ID;
        TicketResponse response = new TicketResponse(
                ticketId,
                "Cannot reset password",
                TicketStatus.IN_PROGRESS,
                Priority.HIGH,
                null,
                NOW,
                NOW,
                "Reset form returns 500.",
                List.of()
        );
        when(ticketService.changeStatus(eq(ticketId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void shouldReturnConflictWhenStatusTransitionIsIllegal() throws Exception {
        UUID ticketId = TICKET_ID;
        when(ticketService.changeStatus(eq(ticketId), any()))
                .thenThrow(new IllegalTicketTransitionException("OPEN", "CLOSED"));

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "CLOSED"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_TICKET_TRANSITION"));
    }

    @Test
    void shouldAddCommentWithLocationHeader() throws Exception {
        UUID ticketId = TICKET_ID;
        UUID commentId = COMMENT_ID;
        CommentResponse response = new CommentResponse(commentId, "Investigating logs.", NOW);
        when(ticketService.addComment(eq(ticketId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "Investigating logs."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "/api/v1/tickets/" + ticketId + "/comments/" + commentId
                ))
                .andExpect(jsonPath("$.id").value(commentId.toString()));
    }

    @Test
    void shouldReturnValidationFailedWhenCommentBodyIsBlank() throws Exception {
        UUID ticketId = TICKET_ID;

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details[0].field").value("body"));
    }

    private TicketResponse ticketResponse(UUID ticketId) {
        return new TicketResponse(
                ticketId,
                "Cannot reset password",
                TicketStatus.OPEN,
                Priority.HIGH,
                null,
                NOW,
                NOW,
                "Reset form returns 500.",
                List.of()
        );
    }
}
