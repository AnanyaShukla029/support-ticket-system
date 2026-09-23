package com.ttn.supporttickets.ticket.repository;

import com.ttn.supporttickets.ticket.entity.Ticket;
import com.ttn.supporttickets.ticket.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

/**
 * Persistence access for {@link Ticket} rows.
 */
public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    @Query("""
            SELECT t
            FROM Ticket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (
                    :keyword = ''
                    OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            """)
    Page<Ticket> search(
            @Param("keyword") String keyword,
            @Param("status") TicketStatus status,
            Pageable pageable
    );
}
