# Support Ticket Management System

Spec-driven Support Ticket Management System with a Java 21 / Spring Boot API,
a React / Next.js frontend, and PostgreSQL persistence.

Product requirements and acceptance criteria are in
[`spec/requirements.md`](spec/requirements.md). The sequential implementation
plan is in [`spec/tasks.md`](spec/tasks.md).

## Prerequisites

- JDK 21
- Maven 3.9 or newer
- Node.js 20 or newer and npm
- PostgreSQL installed locally
- A POSIX-compatible shell

Docker is not required for this project.

## Repository layout

```text
backend/   Spring Boot API
frontend/  Next.js App Router UI
spec/      Source-of-truth product and technical specifications
```

## Local PostgreSQL

Start the PostgreSQL service if it is not already running:

```bash
sudo systemctl start postgresql
pg_isready
```

Create separate runtime and integration-test databases using a PostgreSQL role
available on your machine:

```bash
createdb support_tickets
createdb support_tickets_test
```

If your Linux user is not a PostgreSQL role, run the commands as the local
PostgreSQL administrator:

```bash
sudo -u postgres createdb support_tickets
sudo -u postgres createdb support_tickets_test
```

Do not commit database credentials.

## Environment

Copy the example and replace placeholders locally:

```bash
cp env.properties env.properties
```

The project uses:

- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `APP_CORS_ALLOWED_ORIGINS`
- `NEXT_PUBLIC_API_BASE_URL`

Load the root environment file before running the backend:

```bash
set -a
source env.properties
set +a
```

The backend uses port `8080`; the frontend uses port `3000`.

## Backend

Compile and test from the repository root:

```bash
./mvnw -pl backend test
```

Run the API against local PostgreSQL (default profile `local`):

```bash
set -a
source env.properties
set +a
./mvnw -pl backend spring-boot:run
```

JPA creates and updates tables automatically (`ddl-auto: update`). The app
connects to `support_tickets` using the datasource environment variables above.

Integration tests use the `integration` profile and database
`support_tickets_test`:

```bash
set -a
source env.properties
set +a
./mvnw -pl backend verify -Dgroups=integration
```

## Frontend

```bash
cd frontend
npm install
npm run dev
```

Open <http://localhost:3000>.

## Current implementation status

- **Task 1:** project scaffolding (backend, frontend, env templates)
- **Task 2:** backend cross-cutting (`ApiError`, `GlobalExceptionHandler`,
  CORS, local PostgreSQL profile)
- **Task 3:** JPA persistence (`Ticket`, `Comment`, repositories,
  `TicketRepositoryIT` against `support_tickets_test`)
- **Task 4:** domain (`TicketStatusMachine`), API DTOs, `TicketMapper`,
  unit tests
- **Task 5:** `TicketService` (create, get, update, list, status, comments)
  with `TicketServiceTest`

REST endpoints and UI screens are deferred to subsequent tasks in
[`spec/tasks.md`](spec/tasks.md).
