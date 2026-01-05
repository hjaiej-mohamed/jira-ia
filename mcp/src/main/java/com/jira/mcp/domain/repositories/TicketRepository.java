package com.jira.mcp.domain.repositories;

import com.jira.mcp.domain.models.Ticket;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository abstraction for storing and retrieving {@link Ticket} entities.
 *
 * <p>This interface defines the contract for persistence and similarity search operations, independent
 * of the actual storage backend. Implementations may use relational databases, vector databases
 * (e.g., Qdrant, Milvus), or hybrid storage systems.</p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *     <li>Persist individual and batch tickets</li>
 *     <li>Retrieve semantically similar resolved tickets based on text similarity</li>
 * </ul>
 *
 * <p>Typical usage includes:</p>
 * <pre>{@code
 * Ticket ticket = repository.saveTicket(ticket);
 * List<Ticket> matches = repository.searchTicketByDescription("outage network reboot");
 * }</pre>
 */
@Repository
public interface TicketRepository {

    /**
     * Saves a single {@link Ticket} entity.
     *
     * @param ticket The ticket to persist.
     * @return The saved {@link Ticket}, including any generated identifiers or normalized fields.
     */
    Ticket saveTicket(Ticket ticket);

    /**
     * Saves a batch of {@link Ticket} entities in a single operation.
     *
     * @param tickets A list of tickets to persist.
     * @return The list of saved tickets, preserving order.
     */
    List<Ticket> saveTickets(List<Ticket> tickets);

    /**
     * Performs a similarity search to retrieve tickets that are semantically related to a given cause.
     * <p>
     * Implementations often leverage vector embeddings to compare ticket descriptions,
     * resolutions, or extracted problem statements.
     *
     * @param ticketCause A textual description of the problem (e.g., "login timeout on VPN").
     * @return A list of tickets ordered by similarity relevance (most similar first).
     */
    List<Ticket> searchTicketByDescription(String ticketCause);
}
