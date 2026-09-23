import { apiRequest } from "./client";
import type {
  AddCommentRequest,
  ChangeTicketStatusRequest,
  CommentResponse,
  CreateTicketRequest,
  ListTicketsParams,
  PageResponse,
  TicketResponse,
  TicketSummaryResponse,
  UpdateTicketRequest,
} from "./types";

function toQueryString(params: ListTicketsParams): string {
  const searchParams = new URLSearchParams();

  if (params.q) {
    searchParams.set("q", params.q);
  }
  if (params.status) {
    searchParams.set("status", params.status);
  }
  if (params.page !== undefined) {
    searchParams.set("page", String(params.page));
  }
  if (params.size !== undefined) {
    searchParams.set("size", String(params.size));
  }
  if (params.sort) {
    searchParams.set("sort", params.sort);
  }

  const query = searchParams.toString();
  return query ? `?${query}` : "";
}

export async function listTickets(
  params: ListTicketsParams = {},
): Promise<PageResponse<TicketSummaryResponse>> {
  return apiRequest<PageResponse<TicketSummaryResponse>>(
    `/tickets${toQueryString(params)}`,
  );
}

export async function getTicket(ticketId: string): Promise<TicketResponse> {
  return apiRequest<TicketResponse>(`/tickets/${ticketId}`);
}

export async function createTicket(
  request: CreateTicketRequest,
): Promise<TicketResponse> {
  return apiRequest<TicketResponse>("/tickets", {
    method: "POST",
    body: request,
  });
}

export async function updateTicket(
  ticketId: string,
  request: UpdateTicketRequest,
): Promise<TicketResponse> {
  return apiRequest<TicketResponse>(`/tickets/${ticketId}`, {
    method: "PATCH",
    body: request,
  });
}

export async function changeTicketStatus(
  ticketId: string,
  request: ChangeTicketStatusRequest,
): Promise<TicketResponse> {
  return apiRequest<TicketResponse>(`/tickets/${ticketId}/status`, {
    method: "POST",
    body: request,
  });
}

export async function addComment(
  ticketId: string,
  request: AddCommentRequest,
): Promise<CommentResponse> {
  return apiRequest<CommentResponse>(`/tickets/${ticketId}/comments`, {
    method: "POST",
    body: request,
  });
}
