import type { TicketStatus } from "@/lib/api";
import {
  getAllowedNextStatuses,
  getStatusActionLabel,
  isTerminalStatus,
} from "@/lib/stateMachine";

export interface StatusActionsProps {
  currentStatus: TicketStatus;
  disabled?: boolean;
  onChangeStatus: (status: TicketStatus) => void;
}

export function StatusActions({
  currentStatus,
  disabled = false,
  onChangeStatus,
}: StatusActionsProps) {
  const nextStatuses = getAllowedNextStatuses(currentStatus);

  if (isTerminalStatus(currentStatus)) {
    return (
      <p className="status-note">
        This ticket cannot change status while it is {currentStatus}.
      </p>
    );
  }

  return (
    <div className="status-actions">
      {nextStatuses.map((status) => (
        <button
          key={status}
          type="button"
          className="button button--secondary"
          disabled={disabled}
          onClick={() => onChangeStatus(status)}
        >
          {getStatusActionLabel(status)}
        </button>
      ))}
    </div>
  );
}
