package com.jira.mcp.tools.ticket.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Data Transfer Object (DTO) representing a Ticket for MCP Tools and API interactions.
 *
 * <p>This class is used to:
 * <ul>
 *     <li>Receive ticket data from JSON payloads</li>
 *     <li>Send ticket data back to clients or agents</li>
 *     <li>Convert between domain {@link com.jira.mcp.domain.models.Ticket} objects and external representations</li>
 * </ul>
 *
 * <p>Typical usage includes batch insertion, similarity searches, or LLM-based processing.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TicketDTO {

        /** Unique key identifying the ticket (e.g., "GDIPROD-3751"). Required. */
        private String ticketKey;

        /** Chunk identifier if the ticket text has been split into segments. Required. */
        private String chunkId;

        /** Source field of the ticket text (e.g., "description", "summary", "comment"). Required. */
        private String sourceField;

        /** Creation timestamp of the ticket. Required. */
        private Instant created;

        /** Project or team name associated with the ticket. Required. */
        private String project;

        /** Current status of the ticket (e.g., "Overt", "Fermé"). Required. */
        private String status;

        /** Problem cause extracted or inferred by an AI model (LLM). Optional but recommended for similarity search. */
        private String llmCause;

        /** Solution text, either human-provided or AI-generated. Optional but recommended for agent reference. */
        private String llmSolution;
}
