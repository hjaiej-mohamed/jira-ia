package com.jira.mcp.adapters.ouputs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.mcp.domain.models.Ticket;
import com.jira.mcp.domain.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.List;
import java.util.Map;

/**
 * Adapter for storing and retrieving {@link Ticket} objects in a vector database (e.g., Qdrant) via {@link VectorStore}.
 *
 * <p>This adapter implements the {@link TicketRepository} interface, allowing tickets to be:
 * <ul>
 *     <li>Saved individually or in batches</li>
 *     <li>Queried by similarity using textual cause descriptions</li>
 * </ul>
 *
 * <p>Internally, tickets are converted to {@link Document} objects for vector storage, using
 * {@link ObjectMapper} to serialize/deserialize metadata. The {@code llmCause} field is used as
 * the main text for vector embedding and similarity search.
 */
@Repository
@RequiredArgsConstructor
public class QdrantTicketRepositoryAdapter implements TicketRepository {

    /** VectorStore used for similarity search and storage. */
    private final VectorStore vectorStore;

    /** Jackson ObjectMapper for converting Tickets to/from metadata maps. */
    private final ObjectMapper objectMapper;

    /**
     * Maximum number of search results to return in similarity search.
     * Configurable via {@code rag.similarity.topk}.
     */
    @Value("${rag.similarity.topk}")
    @Min(1)
    @Max(100)
    private int topK;

    /**
     * Saves a single ticket to the vector store.
     *
     * @param ticket Ticket to save
     * @return The same ticket object after storage
     */
    @Override
    public Ticket saveTicket(Ticket ticket) {
        vectorStore.add(List.of(ticketToDocument(ticket)));
        return ticket;
    }

    /**
     * Saves a batch of tickets to the vector store.
     *
     * @param tickets List of tickets to save
     * @return The same list of tickets after storage
     */
    @Override
    public List<Ticket> saveTickets(List<Ticket> tickets) {
        List<Document> documents = tickets.stream()
                .map(this::ticketToDocument)
                .toList();
        vectorStore.add(documents);
        return tickets;
    }

    /**
     * Searches for tickets similar to the given cause description.
     *
     * @param ticketCause Text description of the problem to search for
     * @return List of tickets ordered by similarity (most similar first)
     */
    @Override
    public List<Ticket> searchTicketByDescription(String ticketCause) {
        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(ticketCause)
                        .topK(topK)
                        .build()
        );
        return documents.stream()
                .map(this::documentToTicket)
                .toList();
    }

    /**
     * Converts a {@link Ticket} to a {@link Document} for vector storage.
     * The {@code llmCause} field is used as the text, and the entire ticket is stored as metadata.
     *
     * @param ticket Ticket to convert
     * @return Document ready to be stored in the VectorStore
     */
    private Document ticketToDocument(Ticket ticket) {
        Map<String, Object> ticketMap = objectMapper.convertValue(ticket, Map.class);
        return Document.builder()
                .text(ticket.getLlmCause())
                .metadata(ticketMap)
                .build();
    }

    /**
     * Converts a {@link Document} retrieved from the vector store back into a {@link Ticket}.
     *
     * @param document Document retrieved from VectorStore
     * @return Corresponding Ticket
     */
    private Ticket documentToTicket(Document document) {
        return objectMapper.convertValue(document.getMetadata(), Ticket.class);
    }
}
