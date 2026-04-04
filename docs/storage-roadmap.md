# Storage Roadmap

This repository currently uses file-based H2 as the first persistence layer.
That is a deliberate first step, not the final storage answer.

## Current State

- Session state is persisted in H2.
- RAG chunks and embeddings are persisted in H2.
- Evaluation storage is still in-memory.

## Why H2 Now

- It keeps the companion app self-contained.
- It works well for local restart persistence.
- It avoids introducing production infrastructure before the repo's seam boundaries are stable.

## Next Likely Step

The most realistic next storage target is PostgreSQL.

Suggested order:

1. Migrate evaluation results and traces to PostgreSQL if you want durable analytics.
2. Migrate session state to PostgreSQL if you need multi-process or team-shared state.
3. Move RAG to a real vector store or PostgreSQL with `pgvector` if semantic retrieval becomes more than a chapter demo.

The product lane is already on a PostgreSQL-compatible JDBC schema for conversation persistence, so phase 3 can focus on switching the runtime to a real Postgres deployment and adding auth/identity around it rather than redesigning the product model.

## What H2 Should Not Become

- A pretend production vector database.
- A multi-process shared state backend.
- A hidden dependency that the docs describe as more than a local embedded store.

## Practical Rule

Keep H2 while the repo is still optimizing for chapter readability and local runtime simplicity.
Move to PostgreSQL or a vector store when the persistence seam itself becomes the focus of the work.
