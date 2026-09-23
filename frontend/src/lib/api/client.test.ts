import { afterEach, describe, expect, it, vi } from "vitest";
import { apiRequest } from "./client";
import { ApiClientError } from "./errors";

describe("apiRequest", () => {
  afterEach(() => {
    vi.unstubAllEnvs();
    vi.unstubAllGlobals();
  });

  it("calls the API with the configured base URL", async () => {
    vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080");
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ ok: true }),
    });
    vi.stubGlobal("fetch", fetchMock);

    await apiRequest("/tickets");

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/api/v1/tickets",
      expect.objectContaining({
        headers: expect.objectContaining({
          Accept: "application/json",
        }),
      }),
    );
  });

  it("throws ApiClientError with the API error body", async () => {
    vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 404,
        json: async () => ({
          code: "TICKET_NOT_FOUND",
          message: "Ticket was not found",
          details: [],
          correlationId: "test-id",
        }),
      }),
    );

    await expect(apiRequest("/tickets/missing")).rejects.toEqual(
      new ApiClientError(404, {
        code: "TICKET_NOT_FOUND",
        message: "Ticket was not found",
        details: [],
        correlationId: "test-id",
      }),
    );
  });

  it("uses a generic message when the error body is not JSON", async () => {
    vi.stubEnv("NEXT_PUBLIC_API_BASE_URL", "http://localhost:8080");
    vi.stubGlobal(
      "fetch",
      vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        json: async () => {
          throw new Error("not json");
        },
      }),
    );

    await expect(apiRequest("/tickets")).rejects.toMatchObject({
      status: 500,
      message: "Could not reach the server.",
      code: "INTERNAL_ERROR",
    });
  });
});
