# Closed-Network Operation

This repository is intended to run as an internal backend in a closed network before auth/OIDC is added.
The network boundary is the primary safety boundary in this phase.

## Recommended Entry Points

- `POST /api/v1/assistants/query` - official product backend entrypoint for normal integrations
- `GET /api/v1/assistants/admin/conversations` - operator inspection for persistent product conversations
- `GET /api/v1/assistants/admin/overview` - compact product drift summary for operators

## Secondary Surfaces

- chapter demo seams such as `/api/code-agent/run`, `/multi-agent`, and `/workflow-demo`
- companion/runtime seams such as `/api/agent/run`, `/api/rag/*`, `/api/runtime/*`, and `/admin/evaluations/*`

These surfaces remain useful for book-aligned exploration, runtime inspection, and evaluation, but they are not the primary product entry point. `/api/agent/run` stays available as a runtime/manual seam, while `/api/v1/assistants/...` is the official product contract.

## Deployment Hygiene

- Keep the service on a private network or behind a gateway/reverse proxy.
- Prefer the product lane for normal internal integrations.
- Treat chapter demo and companion seams as secondary surfaces.
- Keep `/api/agent/run` as a secondary runtime/manual seam.
- Keep auth/OIDC/roles/tenancy for phase 3 rather than forcing ad hoc access rules into this phase.
