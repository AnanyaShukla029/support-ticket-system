import React from "react";
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TicketList } from "./TicketList";
import { ApiClientError, listTickets } from "@/lib/api";
import { sampleTicketSummary } from "@/test/testUtils";

vi.mock("@/lib/api", async () => {
  const actual = await vi.importActual<typeof import("@/lib/api")>("@/lib/api");
  return {
    ...actual,
    listTickets: vi.fn(),
  };
});

describe("TicketList", () => {
  beforeEach(() => {
    vi.mocked(listTickets).mockResolvedValue({
      items: [sampleTicketSummary],
      page: 0,
      size: 20,
      totalItems: 1,
      totalPages: 1,
    });
  });

  it("renders ticket rows from the API", async () => {
    render(<TicketList />);

    expect(await screen.findByText("Cannot reset password")).toBeInTheDocument();
    expect(screen.getByText("OPEN")).toBeInTheDocument();
    expect(screen.getByText("Alex")).toBeInTheDocument();
  });

  it("filters tickets by title", async () => {
    const user = userEvent.setup();

    render(<TicketList />);
    await screen.findByText("Cannot reset password");

    await user.type(screen.getByPlaceholderText("Search ticket title"), "reset");
    expect(screen.getByText("Cannot reset password")).toBeInTheDocument();
  });

  it("shows an error banner when the list request fails", async () => {
    vi.mocked(listTickets).mockRejectedValue(
      new ApiClientError(400, {
        code: "INVALID_PAGE",
        message: "page must be greater than or equal to 0",
        details: [],
        correlationId: "",
      }),
    );

    render(<TicketList />);

    expect(
      await screen.findByText("page must be greater than or equal to 0"),
    ).toBeInTheDocument();
  });
});
