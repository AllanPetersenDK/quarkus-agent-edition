# Chapter 5 - RAG

## Chapter Goal

Show retrieval-augmented generation with chunking, embeddings, a vector store, and retrieval.

## Quarkus Translation

The RAG module keeps the chapter mechanics visible without external infrastructure, but the main runtime path now persists chunks and embeddings in file-based H2.

## Central Classes

- `dk.ashlan.agent.rag.Chunker`
- `dk.ashlan.agent.rag.FakeEmbeddingClient`
- `dk.ashlan.agent.rag.InMemoryVectorStore`
- `dk.ashlan.agent.rag.JdbcVectorStore`
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
- The H2-backed vector store persists chunk text, metadata, and embeddings, then ranks rows in Java on retrieval.
- Blank or corrupt persisted metadata or embeddings fail explicitly.

## Demo vs Production

- Demo: fake embeddings and in-memory storage in the chapter demos.
- Runtime default: H2-backed chunk and embedding persistence.
- Production placeholders: real embeddings and a real vector store or database tuned for semantic retrieval.
