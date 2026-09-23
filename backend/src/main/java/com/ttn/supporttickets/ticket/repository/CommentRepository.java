package com.ttn.supporttickets.ticket.repository;

import com.ttn.supporttickets.ticket.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

/**
 * Persistence access for {@link Comment} rows.
 */
public interface CommentRepository extends JpaRepository<Comment, UUID> {

    List<Comment> findByTicket_IdOrderByCreatedAtAsc(UUID ticketId);
}
