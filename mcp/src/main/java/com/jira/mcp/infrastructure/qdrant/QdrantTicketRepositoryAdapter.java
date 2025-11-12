package com.jira.mcp.infrastructure.qdrant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jira.mcp.domain.models.Ticket;
import com.jira.mcp.domain.repositories.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
    @Value("${rag.similarity.threshold}")
    @Min(0)
    @Max(1)
    private float similarityThreshold;

    @Override
    public Ticket saveTicket(Ticket ticket) {

        if(log.isDebugEnabled())
            log.debug("Converting ticket {} to vector document before saving.", ticket.getTicketKey());
        vectorStore.add(List.of(ticketToDocument(ticket)));

        if(log.isInfoEnabled())
            log.info("Saved ticket to vector store: key={}, project={}", ticket.getTicketKey(), ticket.getProject());
        return ticket;
    }

    @Override
    public List<Ticket> saveTickets(List<Ticket> tickets) {

        if (tickets == null || tickets.isEmpty()) {
            if(log.isWarnEnabled())
                log.warn("Attempted to save empty or null ticket list. No operation performed.");
            return tickets;
        }

        if(log.isInfoEnabled())
            log.info("Saving {} tickets to vector store...", tickets.size());

        List<Document> documents = tickets.stream()
                .map(this::ticketToDocument)
                .toList();

        vectorStore.add(documents);
        if(log.isInfoEnabled())
            log.info("Successfully saved {} tickets.", tickets.size());
        return tickets;
    }

    @Override
    public List<Ticket> searchTicketByDescription(String ticketCause) {
        if(log.isInfoEnabled())
            log.info("Performing similarity search for cause: {}", ticketCause);
        if(log.isDebugEnabled())
            log.debug("SearchRequest(topK={}) built for query input.", topK);

        List<Document> documents = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(ticketCause)
                        .topK(topK)
                        .similarityThreshold(similarityThreshold)
                        .build()
        );

        if (documents.isEmpty()) {
            if(log.isWarnEnabled())
                log.warn("No similar tickets found for cause: {}", ticketCause);
        } else {
            if(log.isInfoEnabled())
                log.info("{} similar tickets found for cause: '{}'", documents.size(), ticketCause);
        }

        return documents.stream()
                .map(this::documentToTicket)
                .toList();
    }

    private Document ticketToDocument(Ticket ticket) {

        Map<String, Object> ticketMap = objectMapper.convertValue(ticket, Map.class);
        if(log.isDebugEnabled())
            log.debug("Mapping ticket {} to document format.", ticket.getTicketKey());

        return Document.builder()
                .text(ticket.getLlmCause())
                .metadata(ticketMap)
                .build();
    }

    private Ticket documentToTicket(Document document) {

        if(log.isDebugEnabled())
            log.debug("The score of similarity of the doc {}: {}" ,document.getId(),document.getScore());
        Ticket ticket = objectMapper.convertValue(document.getMetadata(), Ticket.class);
        if(log.isDebugEnabled())
            log.debug("Converted document {} back to ticket: key={}",document.getId(), ticket.getTicketKey());

        return ticket;
    }
}
