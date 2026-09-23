import { describe, expect, it } from "vitest";
import { ApiClientError, toFieldErrors } from "./errors";

describe("toFieldErrors", () => {
  it("maps API details to field messages", () => {
    const fieldErrors = toFieldErrors([
      { field: "title", message: "must not be blank" },
      { field: "priority", message: "must not be null" },
    ]);

    expect(fieldErrors).toEqual({
      title: "must not be blank",
      priority: "must not be null",
    });
  });
});

describe("ApiClientError", () => {
  it("exposes API error fields", () => {
    const error = new ApiClientError(400, {
      code: "VALIDATION_FAILED",
      message: "Request validation failed",
      details: [{ field: "title", message: "must not be blank" }],
      correlationId: "abc",
    });

    expect(error.status).toBe(400);
    expect(error.code).toBe("VALIDATION_FAILED");
    expect(error.details).toHaveLength(1);
    expect(error.message).toBe("Request validation failed");
  });
});
