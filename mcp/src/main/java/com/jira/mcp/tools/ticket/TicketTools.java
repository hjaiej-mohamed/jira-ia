package com.jira.mcp.tools.ticket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.mcp.domain.models.Ticket;
import com.jira.mcp.domain.repositories.TicketRepository;
import com.jira.mcp.tools.ticket.dtos.TicketDTO;
import com.jira.mcp.tools.ticket.mappers.TicketDtoMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service providing tools for managing and querying Ticket data.
 *
 * This service exposes MCP-accessible tools for:
 *  - Inserting tickets in batch from JSON
 *  - Searching resolved tickets by similarity of cause/description
 */
@RequiredArgsConstructor
@Service
public class TicketTools {

    /** Repository for storing and retrieving Ticket domain objects. */
    private final TicketRepository qdrantTicketRepositoryAdapter;

    /** Mapper to convert between Ticket and TicketDTO objects. */
    private final TicketDtoMapper ticketDtoMapper;

    /** JSON object mapper for parsing and writing JSON. */
    private final ObjectMapper objectMapper;


    /**
     * MCP Tool: Insert multiple tickets from a JSON array.
     *
     * @param ticketsJson JSON array containing ticket objects
     * @return JSON array of saved tickets, or an error JSON message
     *
     * Usage example:
     * insert_tickets("[{\"title\": \"Bug A\", \"solution\": \"Restart service\"}]")
     */
    @Tool(
            name = "insert_tickets",
            description = """
            Insert one or multiple support tickets into the incident knowledge base.

            This tool expects a JSON array of ticket objects using the TicketDTO structure.
            Use this tool when adding new historical tickets, syncing from Jira, or teaching the
            system about previously resolved incidents.

            Each object in the array should match the following fields:

            - ticketKey      : Unique ticket reference (e.g., "GDIPROD-12687"). REQUIRED.
            - chunkId        : Identifier if the ticket text was chunked (optional) REQUIRED.
            - sourceField    : Where the text came from (e.g., "description", "comment"). REQUIRED.
            - created        : Timestamp in ISO-8601 format (e.g., "2025-01-15T10:43:00Z") REQUIRED.
            - project        : Project / team name REQUIRED.
            - status         : Ticket status (e.g., "Fermé") REQUIRED.
            - llmCause       : AI-extracted problem cause text REQUIRED.
            - llmSolution    : AI-generated or human provided solution text.

            Example Input:
            [
                    {
                        "ticketKey": " GDIPROD-3751",
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
            @ToolParam(
                    required = true,
                    description = "A JSON array of TicketDTO objects (see tool description for expected structure)."
            )
            String ticketsJson
    ) {
        try {
            // Parse incoming JSON string into a tree structure.
            JsonNode root = objectMapper.readTree(ticketsJson);

            // Validate that input is a JSON array.
            if (!root.isArray()) {
                return "{\"error\": \"Input must be a JSON array\"}";
            }

            // Convert JSON → DTO
            List<TicketDTO> dtos = ticketDtoMapper.mapTicketsJsonToTicketsDTO(root);

            // Convert DTO → Domain model
            List<Ticket> domain = dtos.stream()
                    .map(ticketDtoMapper::ticketDTOtoDomain)
                    .toList();

            // Save tickets in persistence layer (e.g., Qdrant / DB)
            List<Ticket> saved = qdrantTicketRepositoryAdapter.saveTickets(domain);

            // Convert back Domain → DTO for returning clean structured output
            List<TicketDTO> result = saved.stream()
                    .map(ticketDtoMapper::domainToTicketDTO)
                    .toList();

            // Return final DTO list as JSON
            return objectMapper.writeValueAsString(result);

        } catch (Exception e) {
            // Safely encode error message inside JSON string
            return "{\"error\": \"Failed to insert: " +
                    e.getMessage().replace("\"", "\\\"") + "\"}";
        }
    }


    /**
     * MCP Tool: Search for similar previously resolved tickets
     * based on the cause/description text.
     *
     * @param ticketCause Text describing the cause of the issue
     * @return List of TicketDTO representing similar resolved tickets
     *
     * Usage example:
     * similarity_search("Database connection timeout")
     */
    @Tool(
            name = "similarity_search",
            description = "Search for similar resolved tickets with solution using ticket cause"
    )
    public List<TicketDTO> findResolvedTicketsBySimilarCause(
            @ToolParam(required=true, description = "Ticket cause")String ticketCause)
    {

        // Retrieve similar tickets based on vector / embedding similarity search
        List<Ticket> tickets = qdrantTicketRepositoryAdapter.searchTicketByDescription(ticketCause);

        // Convert domain → DTO for API / MCP return format
        return tickets.stream()
                .map(ticketDtoMapper::domainToTicketDTO)
                .toList();
    }
}
