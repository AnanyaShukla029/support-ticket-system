const UUID_PATTERN =
  /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i;

export function isUuid(value: string): boolean {
  return UUID_PATTERN.test(value);
}

export function formatDateTime(value: string): string {
  return new Date(value).toLocaleString();
}

export function displayAssignee(assignee: string | null): string {
  return assignee ?? "—";
}
