# Security Stance

This companion edition is intentionally not a public production deployment.
The current security posture is a documented stance, not a live auth implementation. The repo is expected to sit behind a private network boundary or gateway in this phase, with auth/OIDC/roles/tenancy deferred to phase 3.

## Current Endpoint Stance

| Endpoint | Current role | Security stance |
|---|---|---|
| `POST /api/v1/assistants/query` | Official product backend entrypoint | Preferred internal API for closed-network integrations |
| `GET /api/v1/assistants/admin/*` | Product operator inspection | Internal-only drift and debugging seam |
| `POST /api/agent/run` | Secondary runtime/manual seam for the agent loop | Safe for local/dev use and private deployments; not hardened for public internet exposure |
| `GET /api/agent/tools` | Public companion metadata | Safe to keep open in the companion app |
| `POST /api/code-agent/run` | Code-agent production seam | Treat as internal/admin-only if exposed outside localhost |
| `POST /multi-agent` | Multi-agent production seam | Treat as internal/admin-only if exposed outside localhost |
| `POST /admin/evaluations` | Evaluation/admin seam | Treat as internal/admin-only if exposed outside localhost |
| `POST /admin/evaluations/gaia/run` | GAIA validation/dev seam | Treat as internal/admin-only if exposed outside localhost |
| `GET /workflow-demo` | Demo endpoint | Safe for local/demo use only |

## Why This Is The Current Stance

- The repo is a book companion, not a hardened multi-tenant service.
- `code`, `multiagent`, and `eval` are intentionally isolated seams, but they do not yet have an auth layer.
- The product lane is the intended internal backend surface, while chapter-demo and companion seams remain secondary inspection and comparison paths.
- `/api/agent/run` is a runtime/manual seam and not the canonical product backend.
- The cleanest next step, if these endpoints need real exposure, is Quarkus OIDC with role-based access, not ad hoc checks inside the resources.
- For now, the network boundary, reverse proxy/gateway placement, endpoint hygiene, and bounded defaults are the main safety controls.

## What Is Not Done Yet

- No OIDC provider is configured.
- No role-based access control is enforced at the HTTP layer.
- No admin/user split is modeled in code yet.
- No tenancy model is implemented.

## Recommendation

If you want to expose this beyond local/private network use, add `quarkus-oidc` and then protect:

- `/api/v1/assistants/query` as the official product entrypoint behind auth
- `/api/v1/assistants/admin/*` as operator-only inspection
- `admin` and `eval` endpoints with an admin role
- `/api/code-agent/run` and `multi-agent` with an operator role
- keep `api/agent/run` either public for demos or operator-only for non-demo deployments, but do not treat it as the primary product integration path
