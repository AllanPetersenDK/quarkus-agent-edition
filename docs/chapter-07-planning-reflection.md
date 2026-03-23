# Chapter 7 - Planning and Reflection

## Chapter Goal

Plan before execution and reflect after execution so the agent can improve thin answers.

## Quarkus Translation

The Java edition keeps planning and reflection as explicit services around the main agent runner.

## Central Classes

- `dk.ashlan.agent.planning.ExecutionPlan`
- `dk.ashlan.agent.planning.PlanStep`
- `dk.ashlan.agent.planning.PlannerService`
- `dk.ashlan.agent.planning.ReflectionService`
- `dk.ashlan.agent.planning.PlannedAgentOrchestrator`

## Design Choices

- Planning happens before execution.
- Reflection can reject answers that are too thin.
- The re-entry loop is deterministic in the companion edition so it is easy to test.

## Demo vs Production

- Demo: simple planning and length-based reflection.
- Production placeholder: LLM-generated plans and reviewer-style grading.
