# Chapter 5 - RAG

## Chapter Goal

Show retrieval-augmented generation with chunking, embeddings, a vector store, and retrieval.

## Quarkus Translation

The RAG module is implemented as an in-memory companion edition that keeps the chapter mechanics visible without external infrastructure.

## Central Classes

- `dk.ashlan.agent.rag.Chunker`
- `dk.ashlan.agent.rag.FakeEmbeddingClient`
- `dk.ashlan.agent.rag.InMemoryVectorStore`
- `dk.ashlan.agent.rag.Retriever`
- `dk.ashlan.agent.rag.RagService`
- `dk.ashlan.agent.rag.KnowledgeBaseTool`
- `dk.ashlan.agent.chapters.chapter05.Chapter05Support`
- `dk.ashlan.agent.chapters.chapter05.RagIngestionDemo`
- `dk.ashlan.agent.chapters.chapter05.KnowledgeBaseToolDemo`

## Design Choices

- Top-K retrieval is supported.
- Cosine similarity is used for ranking.
- Fake embeddings are deterministic so tests can validate retrieval.

## Demo vs Production

- Demo: fake embeddings and in-memory storage.
- Production placeholders: real embeddings and a persistent vector store.
