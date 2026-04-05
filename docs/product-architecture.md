# Product Architecture

## Purpose

This repository now has a first small product lane alongside the book companion layers.
The goal is to make the repository feel more like a driftable internal platform without rewriting the chapter spine.
The product lane is designed for closed-network internal use first: it is the official backend entrypoint, while chapter demos and companion seams remain available for book-aligned exploration and operator inspection.

## Layers

| Layer | Role | Examples |
|---|---|---|
| Book core | The from-scratch learning spine for the book | `core`, `llm`, `tools`, `types`, chapters 2-4 |
| Companion/runtime | Chapter-aligned runtime seams and demos | RAG, memory, planning, code, multi-agent, eval, runtime inspection |
| Product | Stable internal product entrypoints built on top of the runtime | `dk.ashlan.agent.product`, `POST /api/v1/assistants/query` |

## Product V1

The first product seam is a document and knowledge assistant query endpoint.
It delegates to the mature runtime capabilities already in the repo:

- RAG for knowledge retrieval
- memory/session for conversation continuity
- planning for lightweight task framing
- reflection for a small quality gate
- observability-friendly metadata for inspection

Product runs are also written into the shared chapter-10 run history so the product lane can be replayed and explained after execution through the same inspection seam as the other runtime lanes.
Phase 2 adds a small JDBC-backed conversation store with a PostgreSQL-compatible schema and a read-only operator seam for inspecting persistent product conversations. The local runtime still uses H2, but the storage contract is now shaped so PostgreSQL can become the natural phase-3 destination.

## Design Rules

- Product code lives in `dk.ashlan.agent.product`.
- Product APIs do not use chapter names in their public contract.
- Chapter demo endpoints stay available for book-aligned exploration, but the product lane is the recommended path for building forward.
- Product code delegates to existing runtime capabilities instead of duplicating them.
- Auth, roles, OIDC, and tenancy are intentionally phase-3 work, so the product contract stays focused on persistence, inspection, and operator readiness first.
- Closed-network hygiene comes from the network boundary, bounded defaults, structured errors, and operator inspection rather than from application-layer auth in this phase.
