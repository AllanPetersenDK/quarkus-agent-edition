# Chapter 10 Evaluation And Monitoring

Chapter 10 is the shared observability and evaluation layer for the runtime lanes in this repo.
It stays deliberately small: the goal is to make important runs replayable and explainable after execution, not to introduce a full monitoring platform.

## What Chapter 10 Covers

- manual runtime runs
- product assistant runs
- code-agent runs
- multi-agent runs
- evaluation runs
- GAIA runs

## Shared Run History

The shared run history is exposed through Swagger-visible inspection seams:

- `GET /api/runtime/runs`
- `GET /api/runtime/runs/{runId}`

Each run record is compact and human-readable. Typical fields include:

- `runId`
- `lane`
- `runType`
- `sessionId`, `conversationId`, or `caseId` when relevant
- `objective` or query summary
- `startTime`, `endTime`, and `durationMs`
- `status` and `outcomeCategory`
- `traceSummary`
- `selectedTraceEntries`
- `toolUsageSummary`
- `qualitySignals`
- source, citation, retrieval, tool, and plan counts when relevant
- approval or rejection details when relevant
- failure reason and error category when relevant

## Lightweight Evaluation

Chapter 10 also adds a lightweight case-based evaluation seam:

- `POST /admin/evaluations/runs`

The evaluation response is designed for manual inspection and contains:

- evaluation run id and summary
- total, passed, and failed counts
- average score
- per-case results
- explanations and failure reasons
- observed signals and selected trace excerpts

## Design Rules

- Chapter 10 builds on the existing runtime lanes instead of replacing them.
- The history layer is shared across manual, product, code, multi-agent, evaluation, and GAIA runs.
- Responses are intentionally compact and replay-friendly.
- The implementation stays lightweight and repo-nær on purpose.
