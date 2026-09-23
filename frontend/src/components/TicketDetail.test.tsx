import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TicketDetail } from "./TicketDetail";
import {
  ApiClientError,
  addComment,
  changeTicketStatus,
  getTicket,
  updateTicket,
} from "@/lib/api";
import { sampleTicket } from "@/test/testUtils";

vi.mock("next/link", () => ({
  default: ({
    children,
    href,
  }: {
    children: React.ReactNode;
    href: string;
  }) => <a href={href}>{children}</a>,
}));

vi.mock("@/lib/api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api")>("@/lib/api");
  return {
    ...actual,
    getTicket: vi.fn(),
    updateTicket: vi.fn(),
    changeTicketStatus: vi.fn(),
    addComment: vi.fn(),
  };
});

describe("TicketDetail", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(getTicket).mockResolvedValue(sampleTicket);
    vi.mocked(updateTicket).mockResolvedValue({
      ...sampleTicket,
      title: "Updated title",
    });
    vi.mocked(changeTicketStatus).mockResolvedValue({
      ...sampleTicket,
      status: "IN_PROGRESS",
    });
    vi.mocked(addComment).mockResolvedValue({
      id: "7c9e6679-7425-40de-944b-e07fc1f90ae7",
      body: "Investigating logs.",
      createdAt: "2026-09-21T10:16:00Z",
    });
  });

  it("shows only allowed status actions for an open ticket", async () => {
    render(<TicketDetail ticketId={sampleTicket.id} />);

    expect(await screen.findByRole("button", { name: "Start progress" })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Cancel ticket" })).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Close ticket" })).not.toBeInTheDocument();
  });

  it("shows a banner when a status transition is rejected", async () => {
    const user = userEvent.setup();
    vi.mocked(changeTicketStatus).mockRejectedValue(
      new ApiClientError(409, {
        code: "ILLEGAL_TICKET_TRANSITION",
        message: "Ticket in status OPEN cannot move to IN_PROGRESS",
        details: [],
        correlationId: "",
      }),
    );

    render(<TicketDetail ticketId={sampleTicket.id} />);
    await screen.findByRole("button", { name: "Start progress" });

    await user.click(screen.getByRole("button", { name: "Start progress" }));

    expect(
      await screen.findByText("Ticket in status OPEN cannot move to IN_PROGRESS"),
    ).toBeInTheDocument();
    expect(screen.getByText(/^Status:/)).toHaveTextContent("OPEN");
  });

  it("saves field updates through PATCH", async () => {
    const user = userEvent.setup();

    render(<TicketDetail ticketId={sampleTicket.id} />);
    await screen.findByDisplayValue("Cannot reset password");

    await user.clear(screen.getByLabelText("Title"));
    await user.type(screen.getByLabelText("Title"), "Updated title");
    await user.click(screen.getByRole("button", { name: "Save" }));

    await waitFor(() => {
      expect(updateTicket).toHaveBeenCalledWith(sampleTicket.id, {
        title: "Updated title",
        description: sampleTicket.description,
        priority: sampleTicket.priority,
        assignee: sampleTicket.assignee,
      });
    });
  });

  it("shows not found for an invalid ticket id", async () => {
    render(<TicketDetail ticketId="not-a-uuid" />);

    expect(await screen.findByText("Ticket not found")).toBeInTheDocument();
    expect(getTicket).not.toHaveBeenCalled();
  });
});
