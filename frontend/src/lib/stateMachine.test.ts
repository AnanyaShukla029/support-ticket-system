import { describe, expect, it } from "vitest";
import { getAllowedNextStatuses } from "./stateMachine";

describe("getAllowedNextStatuses", () => {
  it("returns the five allowed target statuses from the state machine", () => {
    expect(getAllowedNextStatuses("OPEN")).toEqual(["IN_PROGRESS", "CANCELLED"]);
    expect(getAllowedNextStatuses("IN_PROGRESS")).toEqual(["RESOLVED", "CANCELLED"]);
    expect(getAllowedNextStatuses("RESOLVED")).toEqual(["CLOSED"]);
    expect(getAllowedNextStatuses("CLOSED")).toEqual([]);
    expect(getAllowedNextStatuses("CANCELLED")).toEqual([]);
  });
});
