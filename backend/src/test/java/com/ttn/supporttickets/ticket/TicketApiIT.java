package com.ttn.supporttickets.ticket;

import com.ttn.supporttickets.support.IntegrationTestBase;
import com.ttn.supporttickets.ticket.entity.Ticket;
import com.ttn.supporttickets.ticket.enums.TicketStatus;
import com.ttn.supporttickets.ticket.service.TicketStatusMachine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end API flows against PostgreSQL.
 */
class TicketApiIT extends IntegrationTestBase {

    @Autowired
    private TicketStatusMachine statusMachine;

    @Test
    void shouldSupportCreateGetPatchCommentAndList() throws Exception {
        UUID ticketId = createOpenTicket();

        mockMvc.perform(get("/api/v1/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ticketId.toString()))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments").isEmpty());

        mockMvc.perform(patch("/api/v1/tickets/{ticketId}", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated integration title",
                                  "assignee": "Alex"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated integration title"))
                .andExpect(jsonPath("$.assignee").value("Alex"));

        Ticket afterPatch = reloadTicket(ticketId);
        assertThat(afterPatch.getTitle()).isEqualTo("Updated integration title");
        assertThat(afterPatch.getAssignee()).isEqualTo("Alex");

        LocalDateTime updatedAtBeforeComment = afterPatch.getUpdatedAt();

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/comments", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "body": "First comment from integration test"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body").value("First comment from integration test"));

        mockMvc.perform(get("/api/v1/tickets/{ticketId}", ticketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].body").value("First comment from integration test"));

        Ticket afterComment = reloadTicket(ticketId);
        assertThat(afterComment.getUpdatedAt()).isEqualTo(updatedAtBeforeComment);

        mockMvc.perform(get("/api/v1/tickets")
                        .param("q", "Updated integration")
                        .param("status", "OPEN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(ticketId.toString()))
                .andExpect(jsonPath("$.items[0].title").value("Updated integration title"))
                .andExpect(jsonPath("$.totalItems").value(1));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("allStatusPairs")
    void shouldApplyOrRejectStatusTransition(TicketStatus from, TicketStatus to) throws Exception {
        UUID ticketId = createTicketInStatus(from);
        LocalDateTime updatedAtBefore = reloadTicket(ticketId).getUpdatedAt();
        boolean allowed = statusMachine.isAllowed(from, to);

        var response = mockMvc.perform(post("/api/v1/tickets/{ticketId}/status", ticketId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + to + "\"}"));

        if (allowed) {
            response.andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(to.name()));
            Ticket saved = reloadTicket(ticketId);
            assertThat(saved.getStatus()).isEqualTo(to);
            assertThat(saved.getUpdatedAt()).isAfter(updatedAtBefore);
            return;
        }

        response.andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ILLEGAL_TICKET_TRANSITION"));
        Ticket saved = reloadTicket(ticketId);
        assertThat(saved.getStatus()).isEqualTo(from);
        assertThat(saved.getUpdatedAt()).isEqualTo(updatedAtBefore);
    }

    @Test
    void shouldRejectUnknownStatusValue() throws Exception {
        UUID ticketId = createOpenTicket();

        mockMvc.perform(post("/api/v1/tickets/{ticketId}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOT_A_REAL_STATUS\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_STATUS"));

        assertThat(reloadTicket(ticketId).getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void shouldReturnNotFoundForMissingStatusTicket() throws Exception {
        mockMvc.perform(post("/api/v1/tickets/{ticketId}/status", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TICKET_NOT_FOUND"));
    }

    private static Stream<Arguments> allStatusPairs() {
        return Arrays.stream(TicketStatus.values())
                .flatMap(from -> Arrays.stream(TicketStatus.values())
                        .map(to -> Arguments.of(from, to)));
    }
}
