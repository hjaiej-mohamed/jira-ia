package com.jira.mcp.tools.ticket;

import com.jira.mcp.tools.ticket.dtos.TicketDTO;
import lombok.AllArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Service
public class TicketTools {
    @Autowired
    private  VectorStore vectorStore;
    @Tool(description = "Insert data")
    public void insertDocuments() {

        List<Document> documents = List.of(

                new Document(
                        "Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!",
                        Map.of(
                                "title", "Spring AI Article",
                                "category", "technology",
                                "price", 0.0,
                                "timestamp", Instant.now().toString()
                        )
                ),

                new Document(
                        "The World is Big and Salvation Lurks Around the Corner",
                        Map.of(
                                "title", "Inspirational Quote",
                                "category", "philosophy",
                                "price", 5.49,
                                "timestamp", Instant.now().toString()
                        )
                ),

                new Document(
                        "You walk forward facing the past and you turn back toward the future.",
                        Map.of(
                                "title", "Another Quote",
                                "category", "philosophy",
                                "price", 9.99,
                                "timestamp", Instant.now().toString()
                        )
                )
        );

        //  This automatically: embed → store vectors → store payloads in Qdrant
        vectorStore.add(documents);
    }
    @Tool(description = "Search by description")
    public List<Document> search(String description) {

        return vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(description)
                        .topK(1)
                        .build()
        );
    }
    @Tool(description = "Search for similar resolved tickets with solution using ticket description")
    List<TicketDTO> getResolvedSimilarTicketsByDescription(String ticketDescription){
        TicketDTO ticket = TicketDTO.builder()
                .ticketKey("upgrade java to 21.")
                .build();
        return  List.of(ticket );
    }
}
