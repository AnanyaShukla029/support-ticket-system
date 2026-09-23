import type { TicketResponse, TicketSummaryResponse } from "@/lib/api";

export const sampleTicketSummary: TicketSummaryResponse = {
  id: "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  title: "Cannot reset password",
  status: "OPEN",
  priority: "HIGH",
  assignee: "Alex",
  createdAt: "2026-09-21T10:15:30Z",
  updatedAt: "2026-09-21T10:15:30Z",
};

export const sampleTicket: TicketResponse = {
  ...sampleTicketSummary,
  description: "Reset form returns 500.",
  comments: [],
};

export function createSearchParams(query = ""): URLSearchParams {
  return new URLSearchParams(query);
}
