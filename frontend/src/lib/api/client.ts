import { ApiClientError } from "./errors";
import type { ApiError } from "./types";

const API_VERSION_PATH = "/api/v1";

function getApiBaseUrl(): string {
  const baseUrl = process.env.NEXT_PUBLIC_API_BASE_URL;

  if (!baseUrl) {
    throw new Error("NEXT_PUBLIC_API_BASE_URL is not set");
  }

  return baseUrl.replace(/\/$/, "");
}

function buildUrl(path: string): string {
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${getApiBaseUrl()}${API_VERSION_PATH}${normalizedPath}`;
}

async function parseApiError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as ApiError;

    return {
      code: body.code ?? "INTERNAL_ERROR",
      message: body.message ?? "An unexpected error occurred",
      details: Array.isArray(body.details) ? body.details : [],
      correlationId: body.correlationId ?? "",
    };
  } catch {
    return {
      code: "INTERNAL_ERROR",
      message: "Could not reach the server.",
      details: [],
      correlationId: "",
    };
  }
}

export interface ApiRequestOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
}

/** Low-level JSON fetch wrapper for the support ticket API. */
export async function apiRequest<T>(
  path: string,
  options: ApiRequestOptions = {},
): Promise<T> {
  const { body, headers, ...rest } = options;

  const response = await fetch(buildUrl(path), {
    ...rest,
    headers: {
      Accept: "application/json",
      ...(body === undefined ? {} : { "Content-Type": "application/json" }),
      ...headers,
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });

  if (!response.ok) {
    const apiError = await parseApiError(response);
    throw new ApiClientError(response.status, apiError);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return (await response.json()) as T;
}
