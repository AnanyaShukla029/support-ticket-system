import type { ErrorDetail } from "@/lib/api";

export interface ErrorBannerProps {
  message: string;
  details?: ErrorDetail[];
  onRetry?: () => void;
}

/**
 * Shows an API or network error with optional field-level details.
 */
export function ErrorBanner({ message, details = [], onRetry }: ErrorBannerProps) {
  const fieldDetails = details.filter((detail) => detail.field);

  return (
    <div className="error-banner" role="alert">
      <p className="error-banner__message">{message}</p>

      {fieldDetails.length > 0 && (
        <ul className="error-banner__details">
          {fieldDetails.map((detail) => (
            <li key={`${detail.field}-${detail.message}`}>
              <strong>{detail.field}:</strong> {detail.message}
            </li>
          ))}
        </ul>
      )}

      {onRetry && (
        <button type="button" className="button button--secondary" onClick={onRetry}>
          Retry
        </button>
      )}
    </div>
  );
}
