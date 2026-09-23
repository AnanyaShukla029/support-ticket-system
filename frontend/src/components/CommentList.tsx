import type { CommentResponse } from "@/lib/api";
import { formatDateTime } from "@/lib/utils";

export interface CommentListProps {
  comments: CommentResponse[];
}

export function CommentList({ comments }: CommentListProps) {
  if (comments.length === 0) {
    return <p className="comment-empty">No comments yet.</p>;
  }

  return (
    <ul className="comment-list">
      {comments.map((comment) => (
        <li key={comment.id} className="comment-list__item">
          <p className="comment-list__meta">{formatDateTime(comment.createdAt)}</p>
          <p className="comment-list__body">{comment.body}</p>
        </li>
      ))}
    </ul>
  );
}
