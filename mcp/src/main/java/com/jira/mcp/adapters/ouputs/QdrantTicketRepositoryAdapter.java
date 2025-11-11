package com.jira.mcp.adapters.ouputs;

import com.jira.mcp.domain.models.Ticket;
import com.jira.mcp.domain.repositories.TicketRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class QdrantTicketRepositoryAdapter implements TicketRepository {
    @Override
    public void addTicket(Ticket ticket) {

    }

    @Override
    public Ticket saveTickets(List<Ticket> tickets) {
        return null;
    }

    @Override
    public Ticket searchTicketByDescription(String ticket) {
        return null;
    }
}
