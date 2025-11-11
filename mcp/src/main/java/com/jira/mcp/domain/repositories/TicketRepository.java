package com.jira.mcp.domain.repositories;

import com.jira.mcp.domain.models.Ticket;

import java.util.List;

public interface TicketRepository {
    public void addTicket(Ticket ticket);
    public Ticket saveTickets(List<Ticket> tickets);
    public Ticket searchTicketByDescription(String ticket);
}
