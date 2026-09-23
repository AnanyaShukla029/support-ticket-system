"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { CommentList } from "@/components/CommentList";
import { EmptyState } from "@/components/EmptyState";
import { ErrorBanner } from "@/components/ErrorBanner";
import { FieldError } from "@/components/FieldError";
import { LoadingState } from "@/components/LoadingState";
import { StatusActions } from "@/components/StatusActions";
import {
  addComment,
  changeTicketStatus,
  getTicket,
  isApiClientError,
  toFieldErrors,
  updateTicket,
  type Priority,
  type TicketResponse,
  type TicketStatus,
} from "@/lib/api";
import { getRememberedListPath } from "@/lib/listQuery";
import { formatDateTime, isUuid } from "@/lib/utils";

const PRIORITY_OPTIONS: Priority[] = ["LOW", "MEDIUM", "HIGH"];

export interface TicketDetailProps {
  ticketId: string;
}

export function TicketDetail({ ticketId }: TicketDetailProps) {
  const [ticket, setTicket] = useState<TicketResponse | null>(null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [priority, setPriority] = useState<Priority>("MEDIUM");
  const [assignee, setAssignee] = useState("");
  const [commentBody, setCommentBody] = useState("");
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saveErrors, setSaveErrors] = useState<Record<string, string>>({});
  const [saveBanner, setSaveBanner] = useState<string | null>(null);
  const [statusBanner, setStatusBanner] = useState<string | null>(null);
  const [commentError, setCommentError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [changingStatus, setChangingStatus] = useState(false);
  const [addingComment, setAddingComment] = useState(false);

  const applyTicket = useCallback((loaded: TicketResponse) => {
    setTicket(loaded);
    setTitle(loaded.title);
    setDescription(loaded.description);
    setPriority(loaded.priority);
    setAssignee(loaded.assignee ?? "");
  }, []);

  const loadTicket = useCallback(async () => {
    if (!isUuid(ticketId)) {
      setNotFound(true);
      setLoading(false);
      return;
    }

    setLoading(true);
    setLoadError(null);
    setNotFound(false);

    try {
      const loaded = await getTicket(ticketId);
      applyTicket(loaded);
    } catch (error) {
      if (isApiClientError(error) && error.status === 404) {
        setNotFound(true);
      } else {
        setLoadError(
          isApiClientError(error)
            ? error.message
            : "Could not reach the server.",
        );
      }
    } finally {
      setLoading(false);
    }
  }, [applyTicket, ticketId]);

  useEffect(() => {
    void loadTicket();
  }, [loadTicket]);

  async function handleSave(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!ticket) {
      return;
    }

    setSaving(true);
    setSaveErrors({});
    setSaveBanner(null);

    try {
      const updated = await updateTicket(ticket.id, {
        title: title.trim(),
        description: description.trim(),
        priority,
        assignee: assignee.trim() ? assignee.trim() : null,
      });
      applyTicket(updated);
    } catch (error) {
      if (isApiClientError(error)) {
        setSaveErrors(toFieldErrors(error.details));
        if (error.details.length === 0) {
          setSaveBanner(error.message);
        }
        if (error.status === 404) {
          setNotFound(true);
        }
      } else {
        setSaveBanner("Could not reach the server.");
      }
    } finally {
      setSaving(false);
    }
  }

  async function handleStatusChange(nextStatus: TicketStatus) {
    if (!ticket) {
      return;
    }

    setChangingStatus(true);
    setStatusBanner(null);

    try {
      const updated = await changeTicketStatus(ticket.id, { status: nextStatus });
      applyTicket(updated);
    } catch (error) {
      if (isApiClientError(error)) {
        setStatusBanner(error.message);
        if (error.status === 404) {
          setNotFound(true);
        }
      } else {
        setStatusBanner("Could not reach the server.");
      }
    } finally {
      setChangingStatus(false);
    }
  }

  async function handleAddComment(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!ticket) {
      return;
    }

    setAddingComment(true);
    setCommentError(null);

    try {
      const comment = await addComment(ticket.id, { body: commentBody.trim() });
      setTicket({
        ...ticket,
        comments: [...ticket.comments, comment],
      });
      setCommentBody("");
    } catch (error) {
      if (isApiClientError(error)) {
        const fieldErrors = toFieldErrors(error.details);
        setCommentError(fieldErrors.body ?? error.message);
        if (error.status === 404) {
          setNotFound(true);
        }
      } else {
        setCommentError("Could not reach the server.");
      }
    } finally {
      setAddingComment(false);
    }
  }

  if (!isUuid(ticketId) || notFound) {
    return (
      <section className="screen">
        <EmptyState
          title="Ticket not found"
          message="The ticket you requested does not exist or the link is invalid."
        />
        <Link className="button button--secondary" href={getRememberedListPath()}>
          Back to list
        </Link>
      </section>
    );
  }

  if (loading) {
    return <LoadingState message="Loading ticket..." />;
  }

  if (loadError) {
    return (
      <section className="screen">
        <ErrorBanner message={loadError} onRetry={loadTicket} />
      </section>
    );
  }

  if (!ticket) {
    return null;
  }

  return (
    <section className="screen">
      <header className="screen__header">
        <div>
          <h1>{ticket.title}</h1>
          <p className="screen__subtitle">Ticket {ticket.id}</p>
        </div>
        <Link className="button button--secondary" href={getRememberedListPath()}>
          Back to list
        </Link>
      </header>

      <div className="ticket-meta">
        <p><strong>Status:</strong> {ticket.status}</p>
        <p><strong>Created:</strong> {formatDateTime(ticket.createdAt)}</p>
        <p><strong>Updated:</strong> {formatDateTime(ticket.updatedAt)}</p>
      </div>

      {statusBanner && <ErrorBanner message={statusBanner} />}

      <section className="panel">
        <h2>Status actions</h2>
        <StatusActions
          currentStatus={ticket.status}
          disabled={changingStatus}
          onChangeStatus={handleStatusChange}
        />
      </section>

      {saveBanner && <ErrorBanner message={saveBanner} />}

      <form className="ticket-form" onSubmit={handleSave}>
        <h2>Edit fields</h2>

        <label className="field">
          <span className="field__label">Title</span>
          <input
            type="text"
            maxLength={200}
            value={title}
            onChange={(event) => setTitle(event.target.value)}
          />
          <FieldError message={saveErrors.title} />
        </label>

        <label className="field">
          <span className="field__label">Description</span>
          <textarea
            maxLength={10000}
            rows={6}
            value={description}
            onChange={(event) => setDescription(event.target.value)}
          />
          <FieldError message={saveErrors.description} />
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
          <FieldError message={saveErrors.priority} />
        </label>

        <label className="field">
          <span className="field__label">Assignee</span>
          <input
            type="text"
            maxLength={120}
            value={assignee}
            onChange={(event) => setAssignee(event.target.value)}
          />
          <FieldError message={saveErrors.assignee} />
        </label>

        <div className="form-actions">
          <button
            type="submit"
            className="button button--primary"
            disabled={saving}
          >
            {saving ? "Saving..." : "Save"}
          </button>
        </div>
      </form>

      <section className="panel">
        <h2>Comments</h2>
        <CommentList comments={ticket.comments} />

        <form className="comment-form" onSubmit={handleAddComment}>
          <label className="field">
            <span className="field__label">Add comment</span>
            <textarea
              rows={4}
              maxLength={4000}
              value={commentBody}
              onChange={(event) => setCommentBody(event.target.value)}
            />
            <FieldError message={commentError} />
          </label>
          <button
            type="submit"
            className="button button--primary"
            disabled={addingComment || commentBody.trim().length === 0}
          >
            {addingComment ? "Adding..." : "Add comment"}
          </button>
        </form>
      </section>
    </section>
  );
}
