import { Suspense } from "react";
import { TicketList } from "@/components/TicketList";
import { LoadingState } from "@/components/LoadingState";

export default function TicketsPage() {
  return (
    <Suspense fallback={<LoadingState message="Loading tickets..." />}>
      <TicketList />
    </Suspense>
  );
}
