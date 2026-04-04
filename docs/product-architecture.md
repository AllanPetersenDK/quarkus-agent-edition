# Product Architecture

## Purpose

This repository now has a first small product lane alongside the book companion layers.
The goal is to make the repository feel more like a driftable internal platform without rewriting the chapter spine.

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

## Design Rules

- Product code lives in `dk.ashlan.agent.product`.
- Product APIs do not use chapter names in their public contract.
- Chapter demo endpoints stay available for book-aligned exploration, but the product lane is the recommended path for building forward.
- Product code delegates to existing runtime capabilities instead of duplicating them.
