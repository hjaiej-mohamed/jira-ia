package com.jira.mcp.config;

import com.jira.mcp.tools.ticket.TicketTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for MCP Tools.
 *
 * <p>This class registers all tools that are exposed to the MCP (Machine Communication Platform)
 * or AI agents. Tools are typically service classes annotated with {@link org.springframework.ai.tool.annotation.Tool}.
 *
 * <p>Currently, it registers the {@link TicketTools} service as a {@link ToolCallbackProvider},
 * allowing MCP agents to call the {@code insertTickets} and {@code similarity_search} tools.
 */
@Configuration
public class ToolsConfig {

    /** The TicketTools service providing ticket-related MCP tools. */
    @Autowired
    private TicketTools ticketTools2;

    /**
     * Exposes TicketTools methods as MCP tools via MethodToolCallbackProvider.
     *
     * @return ToolCallbackProvider containing all registered ticket tools
     */
    @Bean
    public ToolCallbackProvider ticketsTools() {
        return MethodToolCallbackProvider
                .builder()
                .toolObjects(ticketTools2)
                .build();
    }
}
