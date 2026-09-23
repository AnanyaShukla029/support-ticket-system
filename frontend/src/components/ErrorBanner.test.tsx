import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ErrorBanner } from "./ErrorBanner";

describe("ErrorBanner", () => {
  it("shows the message and field details", () => {
    render(
      <ErrorBanner
        message="Request validation failed"
        details={[{ field: "title", message: "must not be blank" }]}
      />,
    );

    expect(screen.getByRole("alert")).toHaveTextContent("Request validation failed");
    expect(screen.getByText("title:")).toBeInTheDocument();
    expect(screen.getByText("must not be blank")).toBeInTheDocument();
  });

  it("calls onRetry when the retry button is clicked", async () => {
    const user = userEvent.setup();
    const onRetry = vi.fn();

    render(<ErrorBanner message="Could not reach the server." onRetry={onRetry} />);

    await user.click(screen.getByRole("button", { name: "Retry" }));

    expect(onRetry).toHaveBeenCalledOnce();
  });
});
