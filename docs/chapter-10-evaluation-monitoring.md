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

## Demo vs Production

- Demo: in-memory results and traces.
- Runtime default: local evaluation runner with explicit duration measurement.
- Production placeholders: Micrometer, OpenTelemetry, durable evaluation storage, and admin/auth protection if the endpoint is exposed outside the companion app.
