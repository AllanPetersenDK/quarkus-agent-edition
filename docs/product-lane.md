# Product Lane

This repository now treats `/api/v1/assistants` as the canonical product backend contract for the assistant frontend, and `agent-ashlan-app` should integrate there first.

The product lane is intentionally product-shaped:

- write query
- list conversations
- inspect conversation detail
- inspect run detail
- inspect run artifacts
- inspect a compact product overview

The product lane is built on the existing runtime motor:

- RAG
- memory
- planning and reflection
- persisted conversation state
- chapter-10 runtime run history
- product-friendly artifact summaries

## Canonical Endpoints

### Write Path

- `POST /api/v1/assistants/query`

### Read, List, and Detail Paths

- `GET /api/v1/assistants/overview`
- `GET /api/v1/assistants/conversations`
- `GET /api/v1/assistants/conversations/{conversationId}`
- `GET /api/v1/assistants/runs/{runId}`
- `GET /api/v1/assistants/runs/{runId}/artifacts`

## What The Product Lane Returns

The canonical product lane returns small, frontend-friendly DTOs:

- conversation identifiers
- run identifiers
- turn counts
- timestamps
- status and failure metadata
- compact summaries for list views
- planning and reflection metadata
- trace and tool-usage summaries
- source- and artifact-level summaries

## How It Differs From Operator/Admin Seams

The product lane is the normal frontend backend contract and the only recommended product integration path.

The operator/admin lane remains available for read-only inspection and release-gate review, but the product frontend does not need to depend on it for normal operation.
Runtime, chapter-demo, and companion seams remain available for teaching and operator workflows, but they are secondary to the product lane.

## Implementation Notes

- Product conversations are persisted in the existing product conversation store.
- Product runs are read back from the shared chapter-10 runtime run history.
- Product artifacts are lightweight summaries derived from the product run/conversation data.
- No auth, roles, OIDC, or tenancy layer is added in this step.

## Why This Matters

`agent-ashlan-app` can now use the product lane as a thin BFF/UI layer without having to reassemble conversation lists, run detail views, or artifact summaries from chapter-demo or admin-only seams.
