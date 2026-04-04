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
- `dk.ashlan.agent.document.DocumentReadService`
- `dk.ashlan.agent.rag.KnowledgeBaseTool`
- `dk.ashlan.agent.chapters.chapter05.Chapter05Support`
- `dk.ashlan.agent.chapters.chapter05.RagIngestionDemo`
- `dk.ashlan.agent.chapters.chapter05.Chapter05PathIngestDemo`
- `dk.ashlan.agent.chapters.chapter05.KnowledgeBaseToolDemo`

## Design Choices

- Top-K retrieval is supported.
- Cosine similarity is used for ranking.
- Chapter-5 query flow adds a small lexical/entity reranking layer on top of embeddings so entity-matching queries are more stable.
- Answers are built from the best matching chunk instead of concatenating all retrieved chunks.
- Chapter-5 ingest now has a small path-based seam that resolves a workspace file through the shared document-read layer before sending the extracted text into the existing RAG ingest flow.
- Text-like documents now normalize through the shared document-read layer, which handles `txt`, `md`, `csv`, `tsv`, `json`, `html`, `xml`, `properties`, `log`, `ini`, `rst`, `toml`, and common source-like text files in the same read path. PDFs use the same extractor, while audio still goes through transcription when available.
- The chapter-5 file exploration demo and GAIA attachment handling reuse the same text/PDF/audio extraction helpers, so document support stays consistent across chapter flows.
- Fake embeddings are deterministic so tests can validate retrieval.
- The H2-backed vector store persists chunk text, metadata, and embeddings, then ranks rows in Java on retrieval.
- Blank or corrupt persisted metadata or embeddings fail explicitly.

## Demo vs Production

- Demo: fake embeddings and in-memory storage in the chapter demos.
- Chapter-5 path ingest: workspace file path -> shared document-read layer -> RAG ingest.
- Runtime default: H2-backed chunk and embedding persistence.
- Production placeholders: real embeddings and a real vector store or database tuned for semantic retrieval.
