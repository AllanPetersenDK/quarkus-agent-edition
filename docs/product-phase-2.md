# Product Phase 2

Phase 2 turns the first product lane into a small internal platform candidate without adding auth, roles, tenancy, or a broad admin surface.

## What Changed

- Product conversation state is now JDBC-backed.
- The schema is intentionally small and PostgreSQL-compatible.
- Local runtime and chapter smokes still run on the embedded H2 database.
- Product responses now expose conversation creation, turn count, status, and failure reason.
- A small operator seam makes product conversations inspectable after execution.

## Runtime Seams

- `POST /api/v1/assistants/query`
- `GET /api/v1/assistants/admin/conversations`
- `GET /api/v1/assistants/admin/conversations/{conversationId}`

## Design Rules

- Product stays built on RAG, memory, planning, reflection, and runtime observability.
- Chapter 2-4 remain the book core.
- Chapter demo endpoints remain companion surfaces, not the official product path.
- Auth, roles, OIDC, and tenancy are intentionally phase 3 work.

## Why This Is Still Lightweight

- The persistence model is a single conversation-state table.
- The operator lane is read-only.
- Error handling is structured, but not over-designed.
- The product layer is easier to operate now without becoming a separate platform.
