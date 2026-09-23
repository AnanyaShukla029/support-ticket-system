package com.ttn.supporttickets.ticket;

import com.ttn.supporttickets.ticket.entity.Comment;
import com.ttn.supporttickets.ticket.entity.Ticket;

/**
 * Test builders for {@link Comment} entities.
 */
public final class CommentFixtures {

    private CommentFixtures() {
    }

    public static Comment comment(Ticket ticket, String body) {
        Comment comment = new Comment();
        comment.setTicket(ticket);
        comment.setBody(body);
        return comment;
    }
}
