import type { TicketStatus } from "@/lib/api";

export const LIST_QUERY_STORAGE_KEY = "ticketListQuery";

export interface TicketListQuery {
  q: string;
  status: TicketStatus | "";
  page: number;
}

export function parseListQuery(searchParams: URLSearchParams): TicketListQuery {
  const status = searchParams.get("status") ?? "";

  return {
    q: searchParams.get("q") ?? "",
    status: isTicketStatus(status) ? status : "",
    page: Math.max(0, Number.parseInt(searchParams.get("page") ?? "0", 10) || 0),
  };
}

export function buildListSearchParams(query: TicketListQuery): URLSearchParams {
  const params = new URLSearchParams();

  if (query.q) {
    params.set("q", query.q);
  }
  if (query.status) {
    params.set("status", query.status);
  }
  if (query.page > 0) {
    params.set("page", String(query.page));
  }

  return params;
}

export function buildListPath(query: TicketListQuery): string {
  const params = buildListSearchParams(query).toString();
  return params ? `/tickets?${params}` : "/tickets";
}

export function rememberListQuery(query: TicketListQuery): void {
  if (typeof window === "undefined") {
    return;
  }

  sessionStorage.setItem(LIST_QUERY_STORAGE_KEY, buildListPath(query));
}

export function getRememberedListPath(): string {
  if (typeof window === "undefined") {
    return "/tickets";
  }

  return sessionStorage.getItem(LIST_QUERY_STORAGE_KEY) ?? "/tickets";
}

function isTicketStatus(value: string): value is TicketStatus {
  return (
    value === "OPEN" ||
    value === "IN_PROGRESS" ||
    value === "RESOLVED" ||
    value === "CLOSED" ||
    value === "CANCELLED"
  );
}
