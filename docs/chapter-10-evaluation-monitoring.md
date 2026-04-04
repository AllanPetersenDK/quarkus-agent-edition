# Chapter 10 - Evaluation and Monitoring

## Chapter Goal

Add evaluation loops, trace capture, and metrics so the system can be measured.

## Quarkus Translation

The edition keeps evaluation and trace capture in dedicated services instead of mixing them into the main agent flow.

## Central Classes

- `dk.ashlan.agent.eval.EvalCase`
- `dk.ashlan.agent.eval.EvalResult`
- `dk.ashlan.agent.eval.EvaluationRunner`
- `dk.ashlan.agent.eval.AgentTrace`
- `dk.ashlan.agent.eval.AgentTraceService`
- `dk.ashlan.agent.eval.RunMetrics`
- `dk.ashlan.agent.chapters.chapter10.Chapter10Support`
- `dk.ashlan.agent.chapters.chapter10.EvaluationRunDemo`
- `dk.ashlan.agent.chapters.chapter10.TraceDemo`
- `dk.ashlan.agent.chapters.chapter10.MetricsDemo`

## Design Choices

- Evaluation runs on a list of cases.
- Traces are recorded per case.
- Metrics are modeled explicitly so later observability work has a clean target.
- The companion demo measures real wall-clock duration for the evaluation run instead of relying on a synthetic constant.
- The admin evaluation endpoint now uses the same real elapsed-time pattern, so chapter demo and runtime API stay aligned.
- A dedicated GAIA validation/dev seam is available for selected validation cases. It loads GAIA parquet snapshots from either a Hugging Face URL or a local path, resolves attachment presence into trace/context notes, extracts plain text from text-like attachments and PDFs, transcribes common audio attachments when OpenAI audio transcription is configured, and uses deterministic scoring for validation.
- The GAIA loader can read Hugging Face parquet validation files directly, so `GAIA_DATASET_URL` may point at `metadata.level1.parquet` or a broader dataset root. `GAIA_LOCAL_PATH` defaults to `target/gaia-data` for workspace-local snapshots, and `GAIA_DEFAULT_CONFIG`, `GAIA_DEFAULT_SPLIT`, and `GAIA_DEFAULT_LEVEL` provide the selection fallbacks.
- `scripts/run-dev.sh` automatically prepares the workspace-local GAIA snapshot on first use when `GAIA_DATASET_URL` is not set, so local validation/dev can start from the repo root without a separate manual download step.
- OCR and multimodal vision are intentionally not part of this phase; unsupported attachment types remain explicit in trace/debug output.

## Demo vs Production

- Demo: in-memory results and traces.
- Runtime default: local evaluation runner with explicit duration measurement.
- GAIA validation/dev flow: dataset-backed validation cases with attachment-aware discovery/context, deterministic scoring, and per-case trace/debug lookup.
- Production placeholders: Micrometer, OpenTelemetry, durable evaluation storage, and admin/auth protection if the endpoint is exposed outside the companion app.
