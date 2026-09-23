import type { TicketStatus } from "@/lib/api";

const ALLOWED_TRANSITIONS: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ["IN_PROGRESS", "CANCELLED"],
  IN_PROGRESS: ["RESOLVED", "CANCELLED"],
  RESOLVED: ["CLOSED"],
  CLOSED: [],
  CANCELLED: [],
};

const STATUS_LABELS: Record<TicketStatus, string> = {
  OPEN: "Open",
  IN_PROGRESS: "Start progress",
  RESOLVED: "Mark resolved",
  CLOSED: "Close ticket",
  CANCELLED: "Cancel ticket",
};

/** Returns the statuses a ticket may move to from its current status. */
export function getAllowedNextStatuses(status: TicketStatus): TicketStatus[] {
  return ALLOWED_TRANSITIONS[status];
}

/** User-facing label for a status action button. */
export function getStatusActionLabel(status: TicketStatus): string {
  return STATUS_LABELS[status];
}

export function isTerminalStatus(status: TicketStatus): boolean {
  return status === "CLOSED" || status === "CANCELLED";
}
