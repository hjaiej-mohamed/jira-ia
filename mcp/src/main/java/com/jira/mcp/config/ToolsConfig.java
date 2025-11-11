package com.jira.mcp.config;

import com.jira.mcp.tools.ticket.TicketTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolsConfig {
    @Autowired
    private TicketTools ticketRepository;

    @Bean
    ToolCallbackProvider ticketsTools(){
        return MethodToolCallbackProvider
                .builder()
                .toolObjects(ticketRepository)
                .build();
    }
}
