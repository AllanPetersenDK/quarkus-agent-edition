# API

This repository exposes a Swagger-visible surface for selected outer runtime and companion seams.
It does not turn the from-scratch orchestration internals into HTTP endpoints.
Chapter 10 adds a shared run-history and lightweight evaluation seam so the important lanes can be inspected after execution without turning the repo into a monitoring platform.

## OpenAPI And Swagger UI

- OpenAPI JSON/YAML: `http://localhost:8080/openapi`
- Swagger UI: `http://localhost:8080/swagger-ui`

The OpenAPI and Swagger UI setup follows the official Quarkus `smallrye-openapi` approach.

Configured properties:

- `quarkus.smallrye-openapi.path=/openapi`
- `quarkus.swagger-ui.always-include=true`
- `quarkus.swagger-ui.path=/swagger-ui`
- `quarkus.smallrye-openapi.info-title=Quarkus Agent Edition API`
- `quarkus.smallrye-openapi.info-version=0.1.0`
- `quarkus.smallrye-openapi.info-description=Quarkus companion edition of Build an AI Agent from Scratch. Swagger documents the HTTP-exposed outer seams: manual agent runs, tool discovery, runtime health, RAG query and ingest, session and memory inspection, shared runtime run history, evaluation run, GAIA validation/dev run, trace lookup, and the selected LangChain4j companion demos. Manual orchestration internals, selector logic, prompt builders, and low-level storage details remain Java-only unless a specific endpoint exposes them.`

## Swagger Coverage

Swagger now documents the outer runtime and companion seams that are practical to exercise over HTTP.
For chapters 2-4, keep the distinction clear: chapter 2 is the LLM layer, chapter 3 is the tool system, and chapter 4 is the manual agent loop. Swagger-visible runtime or companion seams should be read as overlays on those chapters, not as replacements for the book core.

Covered in Swagger:

- `POST /api/agent/run` - chapter-4 manual-agent core seam
- `POST /api/agent/step` - chapter-4 manual-loop inspection seam
- `POST /api/agent/run/structured` - chapter-4 structured-output seam around the manual loop
- `GET /api/agent/tools` - chapter-3 tool-system discovery seam
- `POST /api/agent/tools/invoke` - chapter-3 direct tool execution seam for Swagger-based tool testing
- `GET /api/runtime/health` - combined readiness and liveness view
- `GET /api/runtime/health/ready` - readiness snapshot
- `GET /api/runtime/health/live` - liveness snapshot
- `GET /api/runtime/sessions/{sessionId}` - session inspection, more naturally chapter 6-oriented than chapter 4-oriented
- `GET /api/runtime/sessions/{sessionId}/memory` - memory inspection, more naturally chapter 6-oriented than chapter 4-oriented
- `POST /api/runtime/sessions/{sessionId}/resume` - chapter-6 pause/resume seam for confirmation-gated tools
- `GET /api/runtime/sessions/{sessionId}/trace` - chapter-4 runtime trace inspection seam
- `POST /api/runtime/context/sliding-window` - chapter-6 sliding-window preview seam
- `POST /api/runtime/memory/recall` - chapter-6 explicit long-term memory retrieval seam
- `POST /api/runtime/memory/conversation-search` - chapter-6 explicit conversation memory retrieval seam
- `POST /api/runtime/context/optimize` - chapter-6 request-time context optimization inspection seam
- `POST /api/rag/ingest` - chapter 5-oriented document ingest into the RAG stack
- `POST /api/rag/ingest/path` - chapter 5-oriented workspace path ingest through the shared document-read layer
- `POST /api/rag/ingest/directory` - chapter 5-oriented bulk directory ingest through the shared document-read layer
- `GET /api/rag/query` - chapter 5-oriented RAG query and answer
- `POST /admin/evaluations` - evaluation run
- `POST /admin/evaluations/runs` - chapter-10 case-based evaluation run
- `POST /admin/evaluations/gaia/run` - GAIA validation/dev run with level filtering and attachment-aware context
- `GET /admin/evaluations/gaia/{taskId}` - GAIA task lookup
- `GET /admin/evaluations/gaia/runs/{runId}` - GAIA run lookup
- `GET /admin/evaluations/{caseId}` - evaluation trace lookup
- `POST /api/companion/langchain4j/run` - LangChain4j companion run
- `POST /api/companion/langchain4j/agentic-demo` - LangChain4j agentic companion demo
- `POST /api/companion/llm/completions` - chapter-02 companion direct chat simulation
- `POST /api/companion/llm/async-batch` - chapter-02 companion async batch demo with bounded concurrency and per-prompt failure isolation
- `POST /api/code-agent/run` - chapter-8 code-agent companion seam for the constrained workspace/code-generation flow
- `GET /api/runtime/sessions/{sessionId}/workspace` - chapter-8 workspace inspection seam
- `GET /api/runtime/sessions/{sessionId}/workspace/files` - chapter-8 workspace file listing seam
- `GET /api/runtime/sessions/{sessionId}/generated-tools` - chapter-8 generated-tool registry seam
- `POST /api/runtime/sessions/{sessionId}/generated-tools/invoke` - chapter-8 generated-tool invocation seam
- `POST /multi-agent` - internal chapter demo for the coordinator/reviewer flow
- `GET /multi-agent/history` - chapter-9 run history lookup seam
- `GET /multi-agent/history/{runId}` - chapter-9 single-run inspection seam
- `POST /api/v1/assistants/query` - first product-lane document/knowledge assistant seam
- `GET /api/v1/assistants/admin/conversations` - product operator seam for persistent conversation summaries
- `GET /api/v1/assistants/admin/conversations/{conversationId}` - product operator seam for persistent conversation detail
- `GET /api/runtime/runs` - shared chapter-10 runtime run-history seam
- `GET /api/runtime/runs/{runId}` - shared chapter-10 single-run inspection seam
- `GET /workflow-demo` - internal deterministic workflow demo

Chapter 7 planning and reflection are visible through the existing runtime/tool seams rather than a new workflow API: the runtime tool registry includes `create-tasks` and `reflection`, `GET /api/agent/tools` now works in the live runtime, and chapter-7 runs surface plan/reflection/replan markers in session trace entries.
The runtime inspection seam now also exposes `GET /api/runtime/sessions/{sessionId}/plan` and `GET /api/runtime/sessions/{sessionId}/reflection` so the current chapter-7 plan and latest reflection/replan signal are visible without adding a separate workflow subsystem. In the current runtime, those inspection seams are meaningfully populated for chapter-7 sessions instead of acting as thin placeholders.
Chapter 8 follows the same companion pattern: the runtime exposes a small code-agent run seam, while workspace state and generated tools remain inspectable through session-scoped endpoints rather than a separate platform.
The first product lane follows the same “small seam first” rule: `POST /api/v1/assistants/query` delegates to RAG, memory, planning, reflection, and session state, while keeping the chapter demo endpoints available as companion surfaces rather than the recommended product path.
The shared chapter-10 run history ties the manual runtime lane, the product lane, the code-agent lane, the multi-agent lane, the GAIA lane, and the evaluation lane together. The visible records stay intentionally compact and human-readable: run id, lane, input summary, timing, status, outcome, trace summary, tool usage, quality signals, and the key approval or failure details.
Chapter 10 is also the shared evaluation and quality gate layer for the product lane: product runs record into the same history seam, and the product evaluation cases can be replayed and explained without the original live response body.

Not covered in Swagger:

- `AgentOrchestrator` loop internals
- `LlmClientSelector`
- request/prompt builders
- manual `ToolRegistry` and `ToolExecutor` wiring
- `JdbcVectorStore` implementation details
- internal chapter helper classes whose value is teaching flow rather than external invocation
- the built-in Quarkus health endpoints under `/q/health`
- the MCP server at `/mcp`

## Product Lane

The first product-oriented public seam is:

- `POST /api/v1/assistants/query`

It is intentionally small and stable. The endpoint is built on the existing RAG, memory, planning, reflection, and session capabilities, while keeping the chapter-demo endpoints available for the companion story.

Product responses expose:

- a conversation reference
- a conversation creation flag
- a conversation turn count
- a final answer
- source citations
- compact memory hints
- a small planning summary
- a lightweight reflection result
- a stable status and failure reason when the pipeline rejects the answer
- product-lane signals for inspection

The chapter demo endpoints remain useful for book-aligned exploration, but they are not the recommended product path.
Product runs are also recorded in the shared chapter-10 run history so a query can be replayed or explained later through the same inspection seam as the other runtime lanes.
Phase 2 makes the product lane a little more driftable: the conversation state is persisted in a JDBC-backed store with a PostgreSQL-compatible schema, while local smoke still uses the embedded H2 runtime database. The product contract stays small, and auth/roles/OIDC/tenancy are deliberately phase-3 work.

### Product Operations

`GET /api/v1/assistants/admin/conversations`

Operator seam that lists persistent product conversations with their latest status, turn count, and quality signals.

`GET /api/v1/assistants/admin/conversations/{conversationId}`

Operator seam that returns the stored product conversation detail, including the full turn list for debugging and release-gate review.

The product request contract is defensive by design:

- `conversationId` is optional and gets normalized into a stable product conversation id when omitted
- `query` is required and bounded with validation
- `topK` is capped to a small safe range
- persistence and pipeline failures return a structured product error response instead of an opaque stack trace

## Chapter 10 Run History And Evaluation

Chapter 10 is the shared observability and evaluation layer for the important runtime lanes.
It stays lightweight on purpose: the goal is to replay and explain runs, not to create a full monitoring platform.

Covered chapter-10 seams:

- `GET /api/runtime/runs`
- `GET /api/runtime/runs/{runId}`
- `POST /admin/evaluations/runs`

The run-history records are small by design and typically include:

- run id
- lane and run type
- session, conversation, or case reference when relevant
- input summary and timing
- status, outcome category, and human-readable outcome
- trace summary and selected trace excerpts
- tool usage and quality signals
- source, citation, retrieval, tool, and planning counts when relevant
- approval or rejection details when relevant
- failure reason and error category when relevant

The case-based evaluation seam returns:

- evaluation run id and summary
- total, passed, and failed counts
- average score
- per-case pass or fail results
- explanations and failure reasons
- observed signals and selected trace excerpts

Manual runtime runs, product runs, code-agent runs, multi-agent runs, evaluation runs, and GAIA runs all write into the same history layer so manual Swagger smoke-tests can inspect them after the fact.

## Endpoint Notes

### Manual Runtime

`POST /api/agent/run`

Runtime API: this is the main REST-exposed manual agent loop and the chapter-4 core seam.
Same-session calls now replay prior role-aware conversation history, so a session can remember user-provided facts across turns without relying on tool memory.
The same endpoint also accepts `toolConfirmations` for the small chapter-6 pause/resume bridge; when those are supplied, the request routes to the existing resume path instead of a fresh run, and an explicit `sessionId` is required so the confirmations do not drift onto an anonymous ephemeral run.

Request body:

```json
{
  "message": "What is 25 * 4?",
  "sessionId": "default"
}
```

Resume-style payload:

```json
{
  "sessionId": "c6-hitl-1",
  "toolConfirmations": [
    {
      "toolCallId": "call-123",
      "approved": true,
      "arguments": {
        "path": "temp.txt"
      },
      "reason": null
    }
  ]
}
```

Response body:

```json
{
  "answer": "25 * 4 = 100",
  "stopReason": "FINAL_ANSWER",
  "iterations": 1,
  "trace": [
    "iteration:1",
    "answer:25 * 4 = 100"
  ]
}
```

Field notes:

- `message` is required and validated with Jakarta Bean Validation.
- `sessionId` is optional for normal runs and becomes ephemeral-safe when omitted or blank in the REST `/api/agent/run` seam. The direct core convenience path still keeps the compatibility `default` fallback for internal callers, so it is not guaranteed to be anonymous-safe.
- `answer` maps directly from `AgentRunResult.finalAnswer()`.
- `stopReason` maps from the existing `StopReason` enum.
- `iterations` and `trace` map directly from the existing agent runtime result.
- When the request pauses on pending tools, the response also includes `pendingToolCalls` with the tool name, arguments, tool-call id, and confirmation message.

`POST /api/agent/step`

Chapter-4 inspection seam around the manual loop. This runs one think/act cycle and returns a structured view of that single step instead of the full loop.

Request body:

```json
{
  "message": "What is 25 * 4?",
  "sessionId": "default"
}
```

Response body:

```json
{
  "sessionId": "default",
  "stepNumber": 1,
  "assistantMessage": null,
  "toolCalls": [
    {
      "toolName": "calculator",
      "arguments": { "expression": "25 * 4" },
      "callId": "call-123"
    }
  ],
  "toolResults": [
    {
      "toolName": "calculator",
      "success": true,
      "output": "100",
      "data": { "output": "100" }
    }
  ],
  "finalAnswer": null,
  "isFinal": false,
  "traceEntries": [
    { "kind": "step", "message": "iteration:1" },
    { "kind": "tool-call", "message": "calculator" },
    { "kind": "tool-result", "message": "100" }
  ]
}
```

`POST /api/runtime/sessions/{sessionId}/resume`

Chapter-6 pause/resume seam for confirmation-gated tools. This endpoint accepts a whitelist of confirmations and hands them back to the existing orchestrator; the orchestrator still owns the multi-pending confirmation logic.

Request body:

```json
{
  "confirmations": [
    {
      "toolCallId": "call-1",
      "approved": true,
      "arguments": { "topic": "chapter 6" },
      "reason": null
    }
  ]
}
```

Response body:

```json
{
  "answer": "resumed",
  "stopReason": "FINAL_ANSWER",
  "iterations": 1,
  "trace": [
    "pending_approved:confirmation-demo:call-1"
  ]
}
```

`POST /api/agent/tools/invoke`

Chapter-3 direct tool execution seam. This endpoint bypasses the manual agent loop so a registered tool can be exercised directly from Swagger.

The runtime tool registry now also includes the lightweight chapter-7 planning/reflection tools (`create-tasks` and `reflection`), so they can be listed and invoked through the same seam without becoming a new public workflow API.
The planning/reflection cycle remains tool-style and context-driven: the visible runtime state is inspected through session endpoints, while the tools themselves stay hidden behind the existing agent loop.

Request body:

```json
{
  "toolName": "calculator",
  "arguments": { "expression": "25 * 4" },
  "sessionId": "default"
}
```

Response body:

```json
{
  "toolName": "calculator",
  "success": true,
  "output": "25 * 4 = 100",
  "data": { "output": "25 * 4 = 100" },
  "sessionId": "default",
  "error": null
}
```

`POST /api/runtime/memory/recall`

Chapter-6 explicit long-term memory retrieval seam. This is the direct Swagger surface for cross-session problem-solving memory.

Request body:

```json
{
  "sessionId": "default",
  "query": "PostgreSQL"
}
```

Response body:

```json
{
  "toolName": "recall-memory",
  "sessionId": "default",
  "query": "PostgreSQL",
  "output": "Problem: ..."
}
```

`POST /api/runtime/memory/conversation-search`

Chapter-6 explicit conversation-memory retrieval seam. This is the direct Swagger surface for the existing conversation-search tool wiring.

Request body:

```json
{
  "sessionId": "default",
  "query": "What did I say about PostgreSQL?"
}
```

Response body:

```json
{
  "toolName": "conversation-search",
  "sessionId": "default",
  "query": "What did I say about PostgreSQL?",
  "output": "..."
}
```

`POST /api/runtime/context/optimize`

Chapter-6 request-time context optimization inspection seam. This endpoint shows how the existing optimizer projects a request before any LLM call, without mutating session state or triggering the agent loop.

Request body:

```json
{
  "messages": [
    { "role": "system", "content": "You are helpful." },
    { "role": "user", "content": "Explain the report" }
  ]
}
```

Response body:

```json
{
  "originalTokenCount": 42,
  "projectedTokenCount": 42,
  "strategy": "none",
  "changed": false,
  "originalMessages": [],
  "projectedMessages": []
}
```

`POST /api/runtime/context/sliding-window`

Chapter-6 sliding-window preview seam. This endpoint isolates the sliding-window strategy so the short-term context track is visible on its own, without changing session state or requiring the full optimizer threshold to trigger.

Request body:

```json
{
  "messages": [
    { "role": "system", "content": "You are helpful." },
    { "role": "user", "content": "one" },
    { "role": "assistant", "content": "two" },
    { "role": "user", "content": "three" },
    { "role": "assistant", "content": "four" },
    { "role": "user", "content": "five" }
  ]
}
```

Response body:

```json
{
  "originalTokenCount": 38,
  "projectedTokenCount": 24,
  "strategy": "sliding-window",
  "changed": true,
  "originalMessages": [],
  "projectedMessages": []
}
```

`POST /api/agent/run/structured`

Chapter-4 structured-output seam around the manual loop. This supports one controlled demo schema named `chapter4-answer` and returns a normalized structured answer plus the raw one-step agent result.

Request body:

```json
{
  "message": "Answer in a single sentence.",
  "sessionId": "default",
  "mode": "chapter4-answer"
}
```

Response body:

```json
{
  "sessionId": "default",
  "mode": "chapter4-answer",
  "validationStatus": "VALIDATED",
  "structuredResult": {
    "answer": "Direct answer: Answer in a single sentence."
  },
  "step": {
    "sessionId": "default",
    "stepNumber": 1,
    "assistantMessage": "Direct answer: Answer in a single sentence.",
    "toolCalls": [],
    "toolResults": [],
    "finalAnswer": "Direct answer: Answer in a single sentence.",
    "isFinal": true,
    "traceEntries": [
      { "kind": "step", "message": "iteration:1" },
      { "kind": "assistant-message", "message": "Direct answer: Answer in a single sentence." }
    ]
  },
  "stopReason": "FINAL_ANSWER"
}
```

### Tool Discovery

`GET /api/agent/tools`

Utility/discovery endpoint: this lists the runtime tool registry and does not execute tools.
The registry is the chapter-3 tool-system seam. It now also includes chapter-5-style filesystem tools for controlled workspace exploration: `inspect_path`, `unzip_file`, `list_files`, `read_file`, and `read_document_file`. `read_media_file` remains available as a compatibility alias. All filesystem access is read-only, bound to the shared workspace root model used by the code workspace tools, and rejects symlink access.

### Runtime Inspection

`GET /api/runtime/health`

Read-only runtime inspection seam that shows readiness and liveness together.

`GET /api/runtime/sessions/{sessionId}`

Read-only session inspection seam that exposes the stored conversation messages.

`GET /api/runtime/sessions/{sessionId}/memory`

Read-only memory inspection seam that returns the relevant memories for a session and query. The response now exposes the structured record fields as well as the raw memory text, so storage-vs-presentation is easier to inspect directly through Swagger. The chapter-6 task-memory backend is persisted and vector-like, powered by embeddings, but retrieval still ranks rows in-process rather than using a dedicated vector index.

`GET /api/runtime/sessions/{sessionId}/trace`

Read-only chapter-4 trace inspection seam that returns the structured step history recorded for the session. Request-prep memory injection appears here as a small `request-prep` trace entry, which keeps auto-injection visible without pretending it was a normal registry tool call.

### RAG

`POST /api/rag/ingest`

Swagger-visible companion seam for document ingest into the repo's RAG stack.

`POST /api/rag/ingest/path`

Swagger-visible chapter-5 companion seam for ingesting a workspace document by path. The endpoint resolves the path against the canonical workspace root, runs it through the shared document-read layer, and ingests the extracted text into RAG. Directory ingest is not supported in this first version; the response makes that explicit.
Structured failures are returned in the response body, including statuses such as `INVALID_PATH`, `SECURITY_VIOLATION`, `DIRECTORY_UNSUPPORTED`, `UNSUPPORTED_TYPE`, and `RESOLUTION_FAILED`, so clients can distinguish user input errors from successful ingest.

Example request body:

```json
{
  "path": "docs/chapter5/sample.pdf",
  "sourceId": "sample-pdf"
}
```

The shared document-read layer now covers text-like files such as `txt`, `md`, `csv`, `tsv`, `json`, `html`, `xml`, `properties`, `log`, `ini`, `rst`, `toml`, and common source-like text files, plus PDFs and supported audio through the same normalization seam used by GAIA and the filesystem tools.
It also covers office-style documents such as `docx`, `pptx`, `xlsx`, and `ipynb`, so path ingest can reuse the same extraction seam for more realistic chapter-5 documents.

`POST /api/rag/ingest/directory`

Swagger-visible chapter-5 companion seam for bulk ingesting a workspace directory. The endpoint resolves the directory against the canonical workspace root, reads each candidate through the shared document layer, and ingests only the documents that were actually read successfully. Unsupported files are reported explicitly in the response instead of stopping the whole batch.

Example request body:

```json
{
  "path": "docs/chapter5/samples",
  "sourceIdPrefix": "samples",
  "recursive": false,
  "maxFiles": 20
}
```

Per-file results report `INGESTED`, `SKIPPED_UNSUPPORTED`, `SKIPPED_DIRECTORY`, `READ_FAILED`, `SECURITY_VIOLATION`, `INVALID_PATH`, or `RESOLUTION_FAILED`, so bulk ingest stays easy to debug while remaining workspace-safe.

`GET /api/rag/query`

Swagger-visible companion seam for querying the stored knowledge base.
The chapter-5 query flow now uses a small hybrid reranker and builds `answer` from the best matching chunk instead of concatenating all retrieved chunks.
Query responses also include `bestChunk` and compact `citations` so clients can see the winning source and the ranked evidence behind the answer without parsing the full chunk list.

Example response:

```json
{
  "query": "Which text mentions PostgreSQL?",
  "answer": "Source docs/postgresql.txt mentions PostgreSQL: PostgreSQL is an open-source relational database.",
  "bestChunk": {
    "sourceId": "docs/postgresql.txt",
    "chunkIndex": 0,
    "text": "PostgreSQL is an open-source relational database.",
    "similarity": 0.92
  },
  "citations": [
    {
      "sourceId": "docs/postgresql.txt",
      "sourcePath": "docs/postgresql.txt",
      "chunkIndex": 0,
      "chunkId": "docs/postgresql.txt:0:abcd1234",
      "fileType": "txt",
      "contentType": "text/plain",
      "documentStatus": "TEXT_EXTRACTED",
      "sectionHint": "PostgreSQL is an open-source relational database.",
      "similarity": 0.92
    }
  ],
  "chunks": [
    {
      "sourceId": "docs/postgresql.txt",
      "chunkIndex": 0,
      "text": "PostgreSQL is an open-source relational database.",
      "metadata": {
        "sourceId": "docs/postgresql.txt",
        "sourcePath": "docs/postgresql.txt",
        "fileType": "txt",
        "contentType": "text/plain",
        "documentStatus": "TEXT_EXTRACTED",
        "chunkId": "docs/postgresql.txt:0:abcd1234"
      },
      "similarity": 0.92
    }
  ]
}
```

### Evaluation

`POST /admin/evaluations`

Internal admin seam that runs chapter evaluation cases and returns results plus metrics.

`POST /admin/evaluations/gaia/run`

GAIA validation/dev seam that loads a real GAIA validation snapshot from either a Hugging Face parquet URL or a local path. The flow resolves attachment presence into trace/context notes, extracts plain text from text-like attachments and PDFs, transcribes common audio attachments when OpenAI audio transcription is available, applies deterministic scoring, and runs the existing manual runtime agent on a selectable subset. Current web or video lookup questions can use the runtime `web-search` tool.
Short entity-style answers are scored more strictly now: long compound responses that mention the expected entity alongside competing alternatives do not pass as easily.
For GAIA validation, questions that clearly expect one named entity also get a small single-entity answer policy and a conservative post-processing step so the runner prefers one precise answer over a broad list when that is safe.
For local dev, the default workspace path is `target/gaia-data`, so a snapshot downloaded into the repo workspace can be used without any machine-specific absolute path.
The recommended local flow is simply `./scripts/run-dev.sh`; if the workspace snapshot is missing, the script downloads the Hugging Face validation tree into `target/gaia-data` first.

Example local request:

```json
{
  "localPath": "target/gaia-data",
  "config": "2023",
  "split": "validation",
  "level": 1,
  "limit": 3,
  "failFast": false
}
```

Example Hugging Face request:

```json
{
  "datasetUrl": "https://huggingface.co/datasets/gaia-benchmark/GAIA/resolve/main/2023/validation/metadata.level1.parquet",
  "level": 1,
  "limit": 3,
  "failFast": false
}
```

`GET /admin/evaluations/gaia/{taskId}`

Read-only lookup for the most recent GAIA case result stored for a given task id.

`GET /admin/evaluations/gaia/runs/{runId}`

Read-only lookup for a stored GAIA run result.

`GET /admin/evaluations/{caseId}`

Read-only trace lookup for the most recent stored evaluation run with the given case id.

### LangChain4j Companion

`POST /api/companion/langchain4j/run`

Framework-backed companion comparison seam that answers a single prompt.

`POST /api/companion/langchain4j/agentic-demo`

Framework-backed agentic comparison seam that runs the chapter 07 planning workflow.

### Chapter-02 Companion LLM

`POST /api/companion/llm/completions`

Book chapter: 2 companion/debug seam for direct chat-style requests through Swagger UI.

`POST /api/companion/llm/async-batch`

Book chapter: 2 companion/debug async batch seam for server-side concurrent direct LLM calls. The endpoint uses bounded concurrency, keeps results in input order, and returns per-prompt errors instead of failing the whole batch.

### Internal Chapter Demos

`POST /api/code-agent/run`

Book chapter: 8 companion seam for the deterministic workspace/code-agent workflow. The run writes workspace-local artifacts, registers a session-scoped generated tool, and records stable Chapter 8 trace markers.

`POST /multi-agent`

Internal chapter demo for the coordinator/reviewer flow. The response now carries the chosen specialist, a route reason, the specialist output, the reviewer signal, and a short coordinator summary so the multi-agent behavior is visible from the HTTP body itself.

`GET /workflow-demo`

Internal deterministic workflow demo.

## Design Notes

- The public API is intentionally thin.
- The REST layer delegates to the existing runtime, RAG, memory, health, and evaluation beans.
- The implementation keeps the existing runtime behavior intact rather than introducing a second agent model.
- Internal chapter demos remain Java classes unless a specific endpoint exposes them as a comparison seam.

## Security Stance

- `POST /api/agent/run` is the main companion API and is safe for local/private use as-is.
- `GET /api/agent/tools` is harmless metadata and can remain open in the companion app.
- `POST /api/rag/ingest`, `GET /api/rag/query`, `POST /admin/evaluations`, and the runtime inspection endpoints should be treated as companion/admin seams until a fuller auth layer is added.
- If those seams need external exposure, add Quarkus OIDC and role checks instead of ad hoc request-time logic.
