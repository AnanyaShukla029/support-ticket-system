import type { ApiError, ErrorDetail } from "./types";

/** Thrown when the API returns a non-success response or cannot be reached. */
export class ApiClientError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details: ErrorDetail[];
  readonly correlationId: string;

  constructor(status: number, apiError: ApiError) {
    super(apiError.message);
    this.name = "ApiClientError";
    this.status = status;
    this.code = apiError.code;
    this.details = apiError.details ?? [];
    this.correlationId = apiError.correlationId ?? "";
  }
}

export function isApiClientError(error: unknown): error is ApiClientError {
  return error instanceof ApiClientError;
}

/** Maps API field details to a simple record for inline form errors. */
export function toFieldErrors(details: ErrorDetail[]): Record<string, string> {
  const fieldErrors: Record<string, string> = {};

  for (const detail of details) {
    if (detail.field) {
      fieldErrors[detail.field] = detail.message;
    }
  }

  return fieldErrors;
}
