package com.jira.mcp.domain.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Ticket {
    private String ticketKey;
    private String chunkId;
    private String sourceField;
    private Instant created;
    private String project;
    private String status;
    private String llmCause;
    private String llmSolution;
}
