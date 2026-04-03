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
- `dk.ashlan.agent.chapters.chapter07.Chapter07Support`
- `dk.ashlan.agent.chapters.chapter07.PlanningDemo`
- `dk.ashlan.agent.chapters.chapter07.ReflectionDemo`
- `dk.ashlan.agent.chapters.chapter07.ImprovementLoopDemo`
- `dk.ashlan.agent.chapters.chapter07.companion.CompanionPlanWorkflow`
- `dk.ashlan.agent.chapters.chapter07.companion.LangChain4jAgenticCompanionDemo`
- `dk.ashlan.agent.chapters.chapter07.companion.LangChain4jToolCallingCompanionDemo`

## Design Choices

- Planning happens before execution.
- Reflection can reject answers that are too thin.
- The re-entry loop is deterministic in the companion edition so it is easy to test.
- The manual planning/reflection loop remains the baseline learning path.
- The LangChain4j agentic demo is a comparison seam that shows a framework-backed workflow without replacing the manual services.
- The LangChain4j tool-calling companion demo is a separate comparison seam that exercises the framework-backed tool path without replacing the manual services.

## Demo vs Production

- Demo: simple planning and length-based reflection.
- Production placeholder: LLM-generated plans and reviewer-style grading.
- Companion demo: `LangChain4jAgenticCompanionDemo` for the framework-backed comparison path.
- Companion tool-calling demo: `LangChain4jToolCallingCompanionDemo` for the framework-backed tool-calling comparison path.
