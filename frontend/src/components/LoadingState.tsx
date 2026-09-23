export interface LoadingStateProps {
  message?: string;
}

/**
 * Simple loading indicator for screens waiting on API data.
 */
export function LoadingState({ message = "Loading..." }: LoadingStateProps) {
  return (
    <p className="loading-state" role="status" aria-live="polite">
      {message}
    </p>
  );
}
