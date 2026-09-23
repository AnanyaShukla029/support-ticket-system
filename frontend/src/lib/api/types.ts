/** Ticket lifecycle status values from the API contract. */
export type TicketStatus =
  | "OPEN"
  | "IN_PROGRESS"
  | "RESOLVED"
  | "CLOSED"
  | "CANCELLED";

/** Ticket priority values from the API contract. */
export type Priority = "LOW" | "MEDIUM" | "HIGH";

/** Standard API error body for 4xx/5xx responses. */
export interface ApiError {
  code: string;
  message: string;
  details: ErrorDetail[];
  correlationId: string;
}

export interface ErrorDetail {
  field: string;
  message: string;
}

export interface CommentResponse {
  id: string;
  body: string;
  createdAt: string;
}

export interface TicketSummaryResponse {
  id: string;
  title: string;
  status: TicketStatus;
  priority: Priority;
  assignee: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TicketResponse extends TicketSummaryResponse {
  description: string;
  comments: CommentResponse[];
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface CreateTicketRequest {
  title: string;
  description: string;
  priority: Priority;
  assignee?: string | null;
}

export interface UpdateTicketRequest {
  title?: string;
  description?: string;
  priority?: Priority;
  assignee?: string | null;
}

export interface ChangeTicketStatusRequest {
  status: TicketStatus;
}

export interface AddCommentRequest {
  body: string;
}

export interface ListTicketsParams {
  q?: string;
  status?: TicketStatus;
  page?: number;
  size?: number;
  sort?: string;
}
