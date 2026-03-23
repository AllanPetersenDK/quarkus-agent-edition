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

## Design Choices

- Evaluation runs on a list of cases.
- Traces are recorded per case.
- Metrics are modeled explicitly so later observability work has a clean target.

## Demo vs Production

- Demo: in-memory results and traces.
- Production placeholders: Micrometer, OpenTelemetry, and durable evaluation storage.
