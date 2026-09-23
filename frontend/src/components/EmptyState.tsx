export interface EmptyStateProps {
  title: string;
  message?: string;
}

/**
 * Shown when a list or section has no data but the request succeeded.
 */
export function EmptyState({ title, message }: EmptyStateProps) {
  return (
    <div className="empty-state">
      <h2>{title}</h2>
      {message && <p>{message}</p>}
    </div>
  );
}
