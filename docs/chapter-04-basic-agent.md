# Chapter 04 - Basic Agent

This chapter maps the Python basic agent loop into the Quarkus core orchestration layer.

## Python Files

- `scratch_agents/agents/execution_context_ch4.py`
- `scratch_agents/agents/tool_calling_agent_ch4_base.py`
- `scratch_agents/agents/tool_calling_agent_ch4_callback.py`
- `scratch_agents/agents/tool_calling_agent_ch4_structured_output.py`
- `chapter_04_basic_agent/01_solve_kipchoge_problem.py`
- `chapter_04_basic_agent/02_agent_structured_output.py`
- `chapter_04_basic_agent/03_human_in_the_loop.py`

## Quarkus Classes

- `dk.ashlan.agent.core.ExecutionContext`
- `dk.ashlan.agent.core.AgentOrchestrator`
- `dk.ashlan.agent.core.CallbackAwareAgentOrchestrator`
- `dk.ashlan.agent.core.StructuredOutputAgentOrchestrator`
- `dk.ashlan.agent.core.AgentRunResult`
- `dk.ashlan.agent.core.AgentRunTrace`
- `dk.ashlan.agent.core.StopReason`
- `dk.ashlan.agent.chapters.chapter04.*`

## Design Notes

- The loop is explicit and testable.
- Tool calls are represented as structured content.
- The Quarkus version keeps the control flow visible rather than hiding it in a large framework.

## Demo vs Production

- Demo: `DemoToolCallingLlmClient`, `StructuredOutputParser`, and the chapter demo classes.
- Production placeholder: provider-backed tool-calling LLMs.
