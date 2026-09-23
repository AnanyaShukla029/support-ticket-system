"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { ErrorBanner } from "@/components/ErrorBanner";
import { FieldError } from "@/components/FieldError";
import {
  createTicket,
  isApiClientError,
  toFieldErrors,
  type Priority,
} from "@/lib/api";

const PRIORITY_OPTIONS: Priority[] = ["LOW", "MEDIUM", "HIGH"];

export function TicketForm() {
  const router = useRouter();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<Priority>("MEDIUM");
  const [assignee, setAssignee] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [bannerMessage, setBannerMessage] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    setFieldErrors({});
    setBannerMessage(null);

    try {
      const ticket = await createTicket({
        title: title.trim(),
        description: description.trim(),
        priority,
        ...(assignee.trim() ? { assignee: assignee.trim() } : {}),
      });

      router.push(`/tickets/${ticket.id}`);
    } catch (error) {
      if (isApiClientError(error)) {
        setFieldErrors(toFieldErrors(error.details));
        if (error.details.length === 0) {
          setBannerMessage(error.message);
        }
      } else {
        setBannerMessage("Could not reach the server.");
      }
      setSubmitting(false);
    }
  }

  return (
    <section className="screen">
      <header className="screen__header">
        <div>
          <h1>New ticket</h1>
          <p className="screen__subtitle">Create a support ticket. Status starts as OPEN.</p>
        </div>
        <Link className="button button--secondary" href="/tickets">
          Cancel
        </Link>
      </header>

      {bannerMessage && <ErrorBanner message={bannerMessage} />}

      <form className="ticket-form" onSubmit={handleSubmit}>
        <label className="field">
          <span className="field__label">Title</span>
          <input
            type="text"
            maxLength={200}
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />
          <FieldError message={fieldErrors.title} />
        </label>

        <label className="field">
          <span className="field__label">Description</span>
          <textarea
            maxLength={10000}
            rows={6}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
          <FieldError message={fieldErrors.description} />
        </label>

        <label className="field">
          <span className="field__label">Priority</span>
          <select
            value={priority}
            onChange={(event) => setPriority(event.target.value as Priority)}
          >
            {PRIORITY_OPTIONS.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
          <FieldError message={fieldErrors.priority} />
        </label>

        <label className="field">
          <span className="field__label">Assignee</span>
          <input
            type="text"
            maxLength={120}
            value={assignee}
            onChange={(event) => setAssignee(event.target.value)}
          />
          <FieldError message={fieldErrors.assignee} />
        </label>

        <div className="form-actions">
          <button
            type="submit"
            className="button button--primary"
            disabled={submitting}
          >
            {submitting ? "Creating..." : "Create ticket"}
          </button>
        </div>
      </form>
    </section>
  );
}
