"use client";

import Link from "next/link";
import React from "react";
import { useEffect, useState } from "react";
import { EmptyState } from "@/components/EmptyState";
import { ErrorBanner } from "@/components/ErrorBanner";
import { LoadingState } from "@/components/LoadingState";
import {
  isApiClientError,
  listTickets,
  type TicketStatus,
  type TicketSummaryResponse,
} from "@/lib/api";
import { displayAssignee, formatDateTime } from "@/lib/utils";

const STATUS_OPTIONS: Array<{ value: TicketStatus | ""; label: string }> = [
  { value: "", label: "All" },
  { value: "OPEN", label: "Open" },
  { value: "IN_PROGRESS", label: "In progress" },
  { value: "RESOLVED", label: "Resolved" },
  { value: "CLOSED", label: "Closed" },
  { value: "CANCELLED", label: "Cancelled" },
];

export function TicketList() {
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState<TicketStatus | "">("");
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [tickets, setTickets] = useState<TicketSummaryResponse[]>([]);

  useEffect(() => {
    async function loadTickets() {
      setLoading(true);
      setErrorMessage(null);

      try {
        const response = await listTickets({ size: 100 });
        setTickets(response.items);
      } catch (error) {
        setErrorMessage(
          isApiClientError(error) ? error.message : "Could not reach the server.",
        );
      } finally {
        setLoading(false);
      }
    }

    void loadTickets();
  }, []);

  const normalizedSearch = search.trim().toLowerCase();
  const items = tickets.filter((ticket) => {
    const matchesSearch =
      !normalizedSearch ||
      ticket.title.toLowerCase().includes(normalizedSearch);
    return matchesSearch && (!status || ticket.status === status);
  });
  const hasFilters = Boolean(search.trim() || status);

  return (
    <section className="screen">
      <header className="screen__header">
        <div>
          <h1>Tickets</h1>
          <p className="screen__subtitle">Search, filter, and open support tickets.</p>
        </div>
        <Link className="button button--primary" href="/tickets/new">
          New ticket
        </Link>
      </header>

      <div className="filters">
        <label className="field">
          <span className="field__label">Search</span>
          <input
            type="search"
            value={search}
            placeholder="Search ticket title"
            onChange={(event) => setSearch(event.target.value)}
          />
        </label>

        <label className="field">
          <span className="field__label">Status</span>
          <select
            value={status}
            onChange={(event) => setStatus(event.target.value as TicketStatus | "")}
          >
            {STATUS_OPTIONS.map((option) => (
              <option key={option.label} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      {errorMessage && <ErrorBanner message={errorMessage} />}

      {loading && <LoadingState message="Loading tickets..." />}

      {!loading && !errorMessage && items.length === 0 && (
        <EmptyState
          title={hasFilters ? "No tickets match" : "No tickets yet"}
          message={
            hasFilters
              ? "Try a different search or status filter."
              : "Create the first ticket to get started."
          }
        />
      )}

      {!loading && items.length > 0 && (
        <>
          <table className="ticket-table">
            <thead>
              <tr>
                <th>Title</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Assignee</th>
                <th>Updated</th>
              </tr>
            </thead>
            <tbody>
              {items.map((ticket) => (
                <tr key={ticket.id}>
                  <td>
                    <Link href={`/tickets/${ticket.id}`}>{ticket.title}</Link>
                  </td>
                  <td>{ticket.status}</td>
                  <td>{ticket.priority}</td>
                  <td>{displayAssignee(ticket.assignee)}</td>
                  <td>{formatDateTime(ticket.updatedAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>

        </>
      )}
    </section>
  );
}
