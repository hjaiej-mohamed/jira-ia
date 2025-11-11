package com.jira.mcp.domain.models;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * Domain model representing a Jira support or incident ticket.
 *
 * <p>This class captures the core ticket information, including:
 * <ul>
 *     <li>Jira key and project association</li>
 *     <li>Chunk ID and source field (if splitting ticket content)</li>
 *     <li>Creation timestamp</li>
 *     <li>Ticket status</li>
 *     <li>AI-extracted cause and solution text</li>
 * </ul>
 *
 * <p>Validations are applied to ensure required fields from Jira are always present.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Ticket {

    /** Unique Jira ticket key (e.g., "GDIPROD-3751"). Required. */
    @NotBlank(message = "ticketKey is required")
    private String ticketKey;

    /** Chunk identifier if the ticket text is split into segments (e.g., "GDIPROD-3751__summary__0"). Required. */
    @NotBlank(message = "chunkId is required")
    private String chunkId;

    /** Source field of the ticket text (e.g., "description", "summary", "comment"). Required. */
    @NotBlank(message = "sourceField is required")
    private String sourceField;

    /** Creation timestamp of the ticket. Required. */
    @NotNull(message = "created timestamp is required")
    private Instant created;

    /** Jira project key or name associated with the ticket. Required. */
    @NotBlank(message = "project is required")
    private String project;

    /** Current status of the ticket (e.g., "Overt", "Fermé"). Required. */
    @NotBlank(message = "status is required")
    private String status;

    /** Problem cause extracted by AI (LLM) or human analysis. Optional but useful for similarity search. */
    @Size(max = 2000, message = "llmCause exceeds max length 2000 chars")
    private String llmCause;

    /** Solution text, either human-provided or AI-generated. Optional but useful for agents. */
    @Size(max = 3000, message = "llmSolution exceeds max length 3000 chars")
    private String llmSolution;
}
