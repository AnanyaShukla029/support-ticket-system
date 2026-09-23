import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { TicketForm } from "./TicketForm";
import { ApiClientError, createTicket } from "@/lib/api";
import { sampleTicket } from "@/test/testUtils";

const pushMock = vi.fn();

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: pushMock }),
}));

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
    createTicket: vi.fn(),
  };
});

describe("TicketForm", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(createTicket).mockResolvedValue(sampleTicket);
  });

  it("navigates to the detail page after a successful create", async () => {
    const user = userEvent.setup();

    render(<TicketForm />);

    await user.type(screen.getByLabelText("Title"), "Cannot reset password");
    await user.type(screen.getByLabelText("Description"), "Reset form returns 500.");
    await user.click(screen.getByRole("button", { name: "Create ticket" }));

    await waitFor(() => {
      expect(createTicket).toHaveBeenCalledWith({
        title: "Cannot reset password",
        description: "Reset form returns 500.",
        priority: "MEDIUM",
      });
      expect(pushMock).toHaveBeenCalledWith(`/tickets/${sampleTicket.id}`);
    });
  });

  it("shows inline field errors for validation failures", async () => {
    const user = userEvent.setup();
    vi.mocked(createTicket).mockRejectedValue(
      new ApiClientError(400, {
        code: "VALIDATION_FAILED",
        message: "Request validation failed",
        details: [{ field: "title", message: "must not be blank" }],
        correlationId: "",
      }),
    );

    render(<TicketForm />);

    await user.click(screen.getByRole("button", { name: "Create ticket" }));

    expect(await screen.findByText("must not be blank")).toBeInTheDocument();
    expect(pushMock).not.toHaveBeenCalled();
  });
});
