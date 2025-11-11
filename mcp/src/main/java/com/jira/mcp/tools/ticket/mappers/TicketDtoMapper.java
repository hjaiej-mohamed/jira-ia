package com.jira.mcp.tools.ticket.mappers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jira.mcp.domain.models.Ticket;
import com.jira.mcp.tools.ticket.dtos.TicketDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
/**
 * Mapper responsible for transforming raw ticket data between:
 *  - JSON input received from external systems (e.g., Jira exports, MCP tool requests)
 *  - {@link TicketDTO} objects used at the application boundary
 *  - {@link Ticket} domain entities used internally and persisted.
 *
 * <p>This component ensures that incoming data is:
 * <ul>
 *     <li>Normalized — especially timestamps, ensuring {@code created} is valid ISO-8601</li>
 *     <li>Validated — required fields must be present and non-empty</li>
 *     <li>Mapped — conversions remain centralized and consistent across the application</li>
 * </ul>
 *
 * <p>This prevents downstream failures during:
 * <ul>
 *     <li>Vector embedding / similarity indexing</li>
 *     <li>Storage into persistence layers (DB or Vector Store)</li>
 *     <li>MCP tool inference and reasoning steps</li>
 * </ul>
 *
 * <p>If validation fails, the mapper throws {@link IllegalArgumentException}.
 * This allows upper layers (MCP Agents, REST APIs) to send structured error feedback
 * while keeping domain code clean.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketDtoMapper {

    /** JSON mapper used for deserialization and object conversion. */
    private final ObjectMapper objectMapper;

    /**
     * Converts a JSON array representing multiple ticket entries into a list of validated {@link TicketDTO} objects.
     *
     * <p>Processing steps:
     * <ol>
     *     <li>Normalize timestamp formats to ensure {@code Instant.parse()} compatibility</li>
     *     <li>Deserialize JSON → List of {@link TicketDTO}</li>
     *     <li>Validate field integrity for each DTO</li>
     * </ol>
     *
     * @param root A JSON array node where each element describes a ticket.
     * @return A validated list of DTOs ready for conversion into domain entities.
     * @throws IOException If the JSON format is malformed.
     * @throws IllegalArgumentException If required fields are missing or invalid.
     */
    public List<TicketDTO> mapTicketsJsonToTicketsDTO(JsonNode root) throws IOException {
        if (log.isDebugEnabled())
            log.debug("Received JSON payload for ticket mapping. Size: {}", root.size());

        // Normalize created timestamp fields early to ensure valid Instant parsing
        for (JsonNode node : root) {
            normalizeCreatedTimestamp((ObjectNode) node);
        }

        List<TicketDTO> dtos;
        try {
            dtos = objectMapper.readValue(
                    root.traverse(),
                    new TypeReference<List<TicketDTO>>() {}
            );
        } catch (Exception e) {
            if (log.isErrorEnabled())
                log.error("Failed to deserialize tickets JSON into DTO list", e);
            throw e;
        }

        // Validate business constraints & completeness
        dtos.forEach(this::validateTicket);

        if (log.isInfoEnabled())
            log.info("Successfully mapped {} tickets to DTOs.", dtos.size());

        return dtos;
    }

    /**
     * Ensures that the "created" field is formatted using ISO-8601 conventions required by {@link Instant}.
     * Handles common variations such as:
     *  - Missing seconds → adds ":00"
     *  - Missing timezone → appends "Z" (UTC)
     *
     * <p>If the timestamp still cannot be parsed, an exception is thrown to surface input data inconsistencies early.
     */
    private void normalizeCreatedTimestamp(ObjectNode node) {
        JsonNode createdNode = node.get("created");
        if (createdNode == null || !createdNode.isTextual()) {
            if (log.isDebugEnabled())
                log.debug("No 'created' timestamp found for node, skipping normalization.");
            return;
        }

        String original = createdNode.asText().trim();
        String normalized = original.replace(" ", "T");

        // Add missing seconds
        if (normalized.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$")) {
            normalized += ":00Z";
        }
        // Add timezone if missing
        if (normalized.matches("^\\d{4}-\\d{2}-\\d{2}T.*$") && !normalized.matches(".*[Z+-].*$")) {
            normalized += "Z";
        }

        try {
            Instant.parse(normalized);
        } catch (DateTimeParseException e) {
            if (log.isWarnEnabled())
                log.warn("Invalid timestamp format detected for 'created': {} (normalized: {})", original, normalized);
            throw new IllegalArgumentException("Invalid created timestamp format: '" + original + "'");
        }

        if (!original.equals(normalized) && log.isDebugEnabled())
            log.debug("Normalized timestamp '{}' → '{}'", original, normalized);

        node.put("created", normalized);
    }

    /**
     * Validates required business fields to ensure the DTO represents a meaningful ticket.
     *
     * @param ticket the DTO instance to validate
     */
    private void validateTicket(TicketDTO ticket) {
        require(ticket.getTicketKey(), "ticketKey");
        require(ticket.getChunkId(), "chunkId");
        require(ticket.getProject(), "project");
        require(ticket.getSourceField(), "sourceField");
        require(ticket.getStatus(), "status");

        if (ticket.getCreated() == null) {
            if (log.isWarnEnabled())
                log.warn("Validation failed: missing created timestamp for {}", ticket.getTicketKey());
            throw new IllegalArgumentException("created timestamp is required");
        }

        // Size safeguards to avoid vector-store poisoning / model hallucination amplification
        if (ticket.getLlmCause() != null && ticket.getLlmCause().length() > 2000) {
            if (log.isWarnEnabled())
                log.warn("llmCause exceeds limit ({} chars) for ticket {}", ticket.getLlmCause().length(), ticket.getTicketKey());
            throw new IllegalArgumentException("llmCause exceeds max length 2000 characters");
        }

        if (ticket.getLlmSolution() != null && ticket.getLlmSolution().length() > 3000) {
            if (log.isWarnEnabled())
                log.warn("llmSolution exceeds limit ({} chars) for ticket {}", ticket.getLlmSolution().length(), ticket.getTicketKey());
            throw new IllegalArgumentException("llmSolution exceeds max length 3000 characters");
        }

        log.debug("Validated ticket {}", ticket.getTicketKey());
    }

    /**
     * Generic required-field check.
     */
    private void require(String value, String field) {
        if (value == null || value.isBlank()) {
            if (log.isWarnEnabled())
                log.warn("Validation failed: {} is required", field);
            throw new IllegalArgumentException(field + " is required");
        }
    }

    /**
     * Converts a DTO into the core {@link Ticket} domain model.
     */
    public Ticket ticketDTOtoDomain(TicketDTO ticketDTO) {
        if (log.isDebugEnabled())
            log.debug("Mapping TicketDTO → Ticket (key={})", ticketDTO.getTicketKey());
        return objectMapper.convertValue(ticketDTO, Ticket.class);
    }

    /**
     * Converts a domain entity back into its external-facing DTO representation.
     */
    public TicketDTO domainToTicketDTO(Ticket ticket) {
        if (log.isDebugEnabled())
            log.debug("Mapping Ticket → TicketDTO (key={})", ticket.getTicketKey());
        return objectMapper.convertValue(ticket, TicketDTO.class);
    }
}