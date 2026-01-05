package com.jira.mcp.tools.ticket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.mcp.domain.models.Ticket;
import com.jira.mcp.domain.repositories.TicketRepository;
import com.jira.mcp.tools.ticket.dtos.TicketDTO;
import com.jira.mcp.tools.ticket.mappers.TicketDtoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service providing tools for managing and querying Ticket data.
 * <p>
 * This service exposes MCP-accessible tools for:
 * - Inserting tickets in batch from JSON
 * - Searching resolved tickets by similarity of cause/description
 * </p>
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class TicketTools {

    private final TicketRepository qdrantTicketRepositoryAdapter;
    private final TicketDtoMapper ticketDtoMapper;
    private final ObjectMapper objectMapper;

    /**
     * MCP Tool: Insert multiple tickets from a JSON array.
     *
     * @param ticketsJson JSON array containing ticket objects
     * @return JSON array of saved tickets, or an error JSON message
     */
    @Tool(
            name = "insert_tickets",
            description = """
            Insert one or multiple support tickets into the incident knowledge base.
            This tool expects a JSON array of TicketDTO objects using the TicketDTO structure.
            Use this tool when adding new historical tickets, syncing from Jira, or teaching the system about previously resolved incidents.
            Each object in the array should match the following fields:
            
            - ticketKey : Unique ticket reference (e.g., "GDIPROD-12687"). REQUIRED.
            - chunkId : Identifier if the ticket text was chunked (optional) REQUIRED.
            - sourceField : Where the text came from (e.g., "description", "comment"). REQUIRED.
            - created : Timestamp in ISO-8601 format (e.g., "2025-01-15T10:43:00Z") REQUIRED.
            - project : Project / team name REQUIRED.
            - status : Ticket status (e.g., "Fermé") REQUIRED.
            - llmCause : AI-extracted problem cause text REQUIRED.
            - llmSolution : AI-generated or human provided solution text.
            
            Example Input:
            [
              {
                "ticketKey": "GDIPROD-3751",
                "chunkId": "GDIPROD-3751__summary__0",
                "sourceField": "summary",
                "created": "1222-02-21T00:00:00Z",
                "project": "GDIPROD",
                "status": "Fermé",
                "llmCause": " Absence de gestion du contrôle de validation sur le fichier GTATDP10",
                "llmSolution": "Supprimer l'ATD créé si celui-ci n'existait pas lors de l'arrivée sur le traitement en modifiant le G2GCOR02 et le G2GCOR06"
              }
            ]
            The tool will store the tickets and return them back in normalized JSON.
            """
    )
    public String insertTickets(
            @ToolParam(required = true, description = "A JSON array of TicketDTO objects (see tool description for expected structure).")
            String ticketsJson) {
        if(log.isInfoEnabled())
            log.info("insertTickets tool invoked with JSON payload of {} characters", ticketsJson.length());

        try {
            JsonNode root = objectMapper.readTree(ticketsJson);
            if(log.isDebugEnabled())
                log.debug("Parsed JSON into JsonNode: isArray = {}", root.isArray());

            if (!root.isArray()) {
                if(log.isWarnEnabled())
                    log.warn("Invalid input: Expected JSON array but received {}", root.getNodeType());
                return "{\"error\": \"Input must be a JSON array\"}";
            }

            int inputCount = root.size();
            if(log.isInfoEnabled())
                log.info("Processing {} ticket(s) from input JSON array", inputCount);

            List<TicketDTO> dtos = ticketDtoMapper.mapTicketsJsonToTicketsDTO(root);
            if(log.isDebugEnabled())
                log.debug("Mapped JSON to {} TicketDTO objects", dtos.size());

            List<String> ticketKeys = dtos.stream()
                    .map(TicketDTO::getTicketKey)
                    .collect(Collectors.toList());
            if(log.isDebugEnabled())
                log.debug("Ticket keys to insert: {}", ticketKeys);

            List<Ticket> domain = dtos.stream()
                    .map(ticketDtoMapper::ticketDTOtoDomain)
                    .toList();
            if(log.isDebugEnabled())
                log.debug("Converted {} DTOs to domain Ticket objects", domain.size());

            List<Ticket> saved = qdrantTicketRepositoryAdapter.saveTickets(domain);
            if(log.isInfoEnabled())
                log.info("Successfully persisted {} ticket(s) to Qdrant repository", saved.size());

            List<TicketDTO> result = saved.stream()
                    .map(ticketDtoMapper::domainToTicketDTO)
                    .toList();

            String resultJson = objectMapper.writeValueAsString(result);
            if(log.isInfoEnabled())
                log.info("insertTickets completed successfully. Returning {} saved ticket(s)", result.size());
            return resultJson;

        } catch (Exception e) {
            if(log.isErrorEnabled())
                log.error("Failed to insert tickets due to unexpected error", e);
            String safeMessage = e.getMessage() != null ? e.getMessage().replace("\"", "\\\"") : "Unknown error";
            return "{\"error\": \"Failed to insert: " + safeMessage + "\"}";
        }
    }

    /**
     * MCP Tool: Search for similar previously resolved tickets
     * based on the cause/description text.
     *
     * @param ticketCause Text describing the cause of the issue
     * @return List of TicketDTO representing similar resolved tickets
     */
    @Tool(name = "similarity_search", description = "Search for similar resolved tickets with solution using ticket cause")
    public List<TicketDTO> findResolvedTicketsBySimilarCause(
            @ToolParam(required = true, description = "Ticket cause") String ticketCause) {
        if(log.isInfoEnabled())
            log.info("similarity_search invoked with cause: [{}]", ticketCause.trim());

        try {
            List<Ticket> tickets = qdrantTicketRepositoryAdapter.searchTicketByDescription(ticketCause);
            if(log.isInfoEnabled())
                log.info("Vector similarity search returned {} matching ticket(s)", tickets.size());

            List<TicketDTO> result = tickets.stream()
                    .map(ticketDtoMapper::domainToTicketDTO)
                    .toList();

            List<String> returnedKeys = result.stream()
                    .map(TicketDTO::getTicketKey)
                    .collect(Collectors.toList());
            if(log.isDebugEnabled())
                log.debug("Returning tickets with keys: {}", returnedKeys);

            return result;

        } catch (Exception e) {
            if(log.isErrorEnabled())
                log.error("Error during similarity search for cause: {}", ticketCause, e);
            return List.of(); // Return empty list on error to avoid breaking MCP flow
        }
    }
}