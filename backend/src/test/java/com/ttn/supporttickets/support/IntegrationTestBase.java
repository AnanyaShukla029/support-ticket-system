package com.ttn.supporttickets.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ttn.supporttickets.ticket.entity.Ticket;
import com.ttn.supporttickets.ticket.enums.TicketStatus;
import com.ttn.supporttickets.ticket.repository.CommentRepository;
import com.ttn.supporttickets.ticket.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shared setup for integration tests against local PostgreSQL ({@code support_tickets_test}).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
@Tag("integration")
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TicketRepository ticketRepository;

    @Autowired
    protected CommentRepository commentRepository;

    @BeforeEach
    void cleanDatabase() {
        commentRepository.deleteAll();
        ticketRepository.deleteAll();
    }

    protected UUID createOpenTicket() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Integration test ticket",
                                  "description": "Created by an integration test",
                                  "priority": "MEDIUM"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return UUID.fromString(body.get("id").asText());
    }

    protected void changeStatus(UUID ticketId, TicketStatus status) throws Exception {
        mockMvc.perform(post("/api/v1/tickets/{ticketId}/status", ticketId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    /**
     * Creates a ticket and moves it to the requested status using only allowed transitions.
     */
    protected UUID createTicketInStatus(TicketStatus status) throws Exception {
        UUID ticketId = createOpenTicket();
        if (status == TicketStatus.OPEN) {
            return ticketId;
        }
        if (status == TicketStatus.IN_PROGRESS) {
            changeStatus(ticketId, TicketStatus.IN_PROGRESS);
            return ticketId;
        }
        if (status == TicketStatus.RESOLVED) {
            changeStatus(ticketId, TicketStatus.IN_PROGRESS);
            changeStatus(ticketId, TicketStatus.RESOLVED);
            return ticketId;
        }
        if (status == TicketStatus.CLOSED) {
            changeStatus(ticketId, TicketStatus.IN_PROGRESS);
            changeStatus(ticketId, TicketStatus.RESOLVED);
            changeStatus(ticketId, TicketStatus.CLOSED);
            return ticketId;
        }
        if (status == TicketStatus.CANCELLED) {
            changeStatus(ticketId, TicketStatus.CANCELLED);
            return ticketId;
        }
        throw new IllegalArgumentException("Unknown status: " + status);
    }

    protected Ticket reloadTicket(UUID ticketId) {
        return ticketRepository.findById(ticketId).orElseThrow();
    }
}
