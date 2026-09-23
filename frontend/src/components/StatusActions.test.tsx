import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { StatusActions } from "./StatusActions";

describe("StatusActions", () => {
  it("renders terminal status copy for closed tickets", () => {
    render(
      <StatusActions
        currentStatus="CLOSED"
        onChangeStatus={vi.fn()}
      />,
    );

    expect(
      screen.getByText("This ticket cannot change status while it is CLOSED."),
    ).toBeInTheDocument();
  });

  it("calls onChangeStatus when an allowed action is clicked", async () => {
    const user = userEvent.setup();
    const onChangeStatus = vi.fn();

    render(
      <StatusActions
        currentStatus="OPEN"
        onChangeStatus={onChangeStatus}
      />,
    );

    await user.click(screen.getByRole("button", { name: "Start progress" }));

    expect(onChangeStatus).toHaveBeenCalledWith("IN_PROGRESS");
  });
});
