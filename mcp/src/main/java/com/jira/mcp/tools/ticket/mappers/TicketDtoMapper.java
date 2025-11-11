package com.jira.mcp.tools.ticket.mappers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jira.mcp.domain.models.Ticket;
import com.jira.mcp.tools.ticket.dtos.TicketDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

/**
 * Mapper responsible for converting between raw JSON ticket data, TicketDTO, and Ticket domain models.
 *
 * <p>This mapper is typically used by MCP Tools to:
 * <ul>
 *     <li>Normalize incoming JSON payloads (especially timestamps)</li>
 *     <li>Validate required fields before indexing or embedding</li>
 *     <li>Convert DTO objects to domain entities (and vice-versa)</li>
 * </ul>
 *
 * <p>Normalization:
 * The mapper ensures that the {@code created} date is compliant with ISO-8601 format,
 * adding missing seconds or timezone where necessary to ensure successful deserialization into {@link java.time.Instant}.
 *
 * <p>Validation:
 * If any required field is missing, an {@link IllegalArgumentException} is thrown,
 * which allows MCP agents and APIs to return meaningful structured errors.
 */
@Component
@RequiredArgsConstructor
public class TicketDtoMapper {

    private final ObjectMapper objectMapper;

    /**
     * Converts a JSON array of ticket-like objects into a validated list of {@link TicketDTO}.
     * <p>
     * Performs three steps:
     * <ol>
     *     <li>Normalizes the {@code created} timestamp to a standard ISO-8601 format</li>
     *     <li>Deserializes JSON → List of {@link TicketDTO}</li>
     *     <li>Validates each ticket object and throws meaningful errors if required fields are missing</li>
     * </ol>
     *
     * @param root A JSON array node representing an array of ticket objects.
     * @return A validated list of {@link TicketDTO}.
     * @throws IOException If JSON parsing fails.
     * @throws IllegalArgumentException If any field-level validation fails.
     */
    public List<TicketDTO> mapTicketsJsonToTicketsDTO(JsonNode root) throws IOException {

        // 1. Normalize "created" date field to ISO-8601 if incomplete.
        for (JsonNode node : root) {
            JsonNode createdNode = node.get("created");

            if (createdNode != null && createdNode.isTextual()) {
                String createdStr = createdNode.asText().trim();

                // YYYY-MM-DDTHH:MM → add seconds + Z
                if (createdStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}$")) {
                    createdStr += ":00Z";
                }
                // YYYY-MM-DDTHH:MM:SS → add Z if missing timezone
                else if (createdStr.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$")
                        && !createdStr.matches(".*[Z+-].*")) {
                    createdStr += "Z";
                }

                ((ObjectNode) node).put("created", createdStr);
            }
        }

        // 2. Deserialize JSON → List<TicketDTO>
        List<TicketDTO> ticketDTOS = objectMapper.readValue(
                root.traverse(),
                new TypeReference<>() {}
        );

        // 3. Validate each ticket for required fields
        ticketDTOS.forEach(this::validateTicket);

        return ticketDTOS;
    }

    /**
     * Validates required TicketDTO fields.
     * <p>If any required property is missing or empty, an exception is thrown.</p>
     *
     * @param ticket The ticket DTO object to validate.
     */
    private void validateTicket(TicketDTO ticket) {

        if (ticket == null) {
            throw new IllegalArgumentException("Ticket cannot be null");
        }

        if (ticket.getTicketKey() == null || ticket.getTicketKey().isBlank()) {
            throw new IllegalArgumentException("ticketKey is required");
        }

        if (ticket.getChunkId() == null || ticket.getChunkId().isBlank()) {
            throw new IllegalArgumentException("chunkId is required");
        }

        if (ticket.getProject() == null || ticket.getProject().isBlank()) {
            throw new IllegalArgumentException("project is required");
        }

        if (ticket.getSourceField() == null || ticket.getSourceField().isBlank()) {
            throw new IllegalArgumentException("sourceField is required");
        }

        if (ticket.getCreated() == null) {
            throw new IllegalArgumentException("created timestamp is required");
        }

        if (ticket.getStatus() == null || ticket.getStatus().isBlank()) {
            throw new IllegalArgumentException("status is required");
        }

        // Optional long-field validation
        if (ticket.getLlmCause() != null && ticket.getLlmCause().length() > 2000) {
            throw new IllegalArgumentException("llmCause exceeds max length 2000 characters");
        }

        if (ticket.getLlmSolution() != null && ticket.getLlmSolution().length() > 3000) {
            throw new IllegalArgumentException("llmSolution exceeds max length 3000 characters");
        }
    }

    /**
     * Converts a {@link TicketDTO} to the domain {@link Ticket} entity.
     */
    public Ticket ticketDTOtoDomain(TicketDTO ticketDTO) {
        return objectMapper.convertValue(ticketDTO, Ticket.class);
    }

    /**
     * Converts a domain {@link Ticket} to a {@link TicketDTO}.
     */
    public TicketDTO domainToTicketDTO(Ticket ticket) {
        return objectMapper.convertValue(ticket, TicketDTO.class);
    }
}
