"use client";

import { useCallback, useEffect, useState } from "react";
import { ErrorBanner } from "@/components/ErrorBanner";
import { EmptyState } from "@/components/EmptyState";
import { LoadingState } from "@/components/LoadingState";
import { isApiClientError, listTickets } from "@/lib/api";

/**
 * Lightweight check that the browser can reach the Spring API through CORS.
 */
export function ApiConnectionCheck() {
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [ticketCount, setTicketCount] = useState<number | null>(null);

  const loadTickets = useCallback(async () => {
    setLoading(true);
    setErrorMessage(null);

    try {
      const page = await listTickets();
      setTicketCount(page.totalItems);
    } catch (error) {
      if (isApiClientError(error)) {
        setErrorMessage(error.message);
      } else {
        setErrorMessage("Could not reach the server.");
      }
      setTicketCount(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadTickets();
  }, [loadTickets]);

  if (loading) {
    return <LoadingState message="Connecting to API..." />;
  }

  if (errorMessage) {
    return <ErrorBanner message={errorMessage} onRetry={loadTickets} />;
  }

  return (
    <EmptyState
      title="API connected"
      message={`Found ${ticketCount ?? 0} ticket(s) via GET /api/v1/tickets.`}
    />
  );
}
