# Python to Quarkus Mapping

This document maps the Python reference zip from the book to the Quarkus companion edition in this repository.

## Mapping Legend

- Direct port: the Java class mirrors the Python concept closely.
- Adapted port: the Java class keeps the idea but reshapes it for Quarkus or Java.
- Quarkus companion extension: the Java class has no direct Python equivalent in the zip and extends the reference implementation.

## `chapter_02_llm/`

- `01_llm_chat.py` -> `src/main/java/dk/ashlan/agent/chapters/chapter02/LlmChatDemo.java` - Adapted port
- `02_conversation_management.py` -> `src/main/java/dk/ashlan/agent/chapters/chapter02/ConversationManagementDemo.java` - Direct port
- `03_structured_output.py` -> `src/main/java/dk/ashlan/agent/chapters/chapter02/StructuredOutputDemo.java` - Adapted port
- `04_asynchronous_llm_call.py` -> `src/main/java/dk/ashlan/agent/chapters/chapter02/AsyncLlmCallDemo.java` - Adapted port
- `05_potato_problem.py` -> `src/main/java/dk/ashlan/agent/chapters/chapter02/PotatoProblemDemo.java` - Direct port
- `scratch_agents/models/base_llm.py` -> `src/main/java/dk/ashlan/agent/llm/LlmClient.java` - Adapted port
- `scratch_agents/models/base_llm.py` -> `src/main/java/dk/ashlan/agent/llm/BaseLlmClient.java` - Direct compatibility port
- `scratch_agents/models/llm_request.py` -> `src/main/java/dk/ashlan/agent/llm/LlmRequest.java` - Direct port
- `scratch_agents/models/llm_response.py` -> `src/main/java/dk/ashlan/agent/llm/LlmResponse.java` - Direct port
- `scratch_agents/models/openai.py` -> `src/main/java/dk/ashlan/agent/llm/OpenAiLlmClient.java` - Adapted port
- `scratch_agents/models/*` -> `src/main/java/dk/ashlan/agent/llm/LlmModelConfig.java` - Quarkus companion extension
- `scratch_agents/agents/execution_context_ch4.py` -> `src/main/java/dk/ashlan/agent/agents/ExecutionContextCh4.java` - Direct port
- `scratch_agents/agents/execution_context_ch6.py` -> `src/main/java/dk/ashlan/agent/agents/ExecutionContextCh6.java` - Direct port
- `scratch_agents/agents/conversation_state.py` -> `src/main/java/dk/ashlan/agent/agents/ConversationState.java` - Adapted port
- `scratch_agents/agents/tool_calling_agent_ch4_base.py` -> `src/main/java/dk/ashlan/agent/agents/ToolCallingAgentCh4Base.java` - Adapted port
- `scratch_agents/agents/tool_calling_agent_ch4_callback.py` -> `src/main/java/dk/ashlan/agent/agents/ToolCallingAgentCh4Callback.java` - Adapted port
- `scratch_agents/agents/tool_calling_agent_ch4_structured_output.py` -> `src/main/java/dk/ashlan/agent/agents/ToolCallingAgentCh4StructuredOutput.java` - Adapted port
- `scratch_agents/agents/tool_calling_agent_ch6.py` -> `src/main/java/dk/ashlan/agent/agents/ToolCallingAgentCh6.java` - Adapted port

## `chapter_03_tool_use/`

- calculator -> `src/main/java/dk/ashlan/agent/tools/CalculatorTool.java` - Direct port
- Tavily search -> `src/main/java/dk/ashlan/agent/tools/WebSearchTool.java` - Adapted port
- Wikipedia -> `src/main/java/dk/ashlan/agent/chapters/chapter03/WikipediaToolDemo.java` and later `tools/WikipediaTool.java` placeholder support - Adapted port
- tool definition -> `src/main/java/dk/ashlan/agent/tools/ToolDefinition.java` - Direct port
- tool abstraction -> `src/main/java/dk/ashlan/agent/tools/Tool.java` - Direct port
- tool decorator -> `src/main/java/dk/ashlan/agent/chapters/chapter03/ToolDecoratorDemo.java` - Adapted port
- `scratch_agents/tools/*.py` -> `src/main/java/dk/ashlan/agent/tools/*` - Adapted port
- `scratch_agents/tools/function_tool.py` -> `src/main/java/dk/ashlan/agent/tools/FunctionToolAdapter.java` - Adapted port
- `scratch_agents/tools/decorator.py` -> `src/main/java/dk/ashlan/agent/tools/AbstractTool.java` and chapter demo wrappers - Adapted port

## `chapter_04_basic_agent/`

- `solve problem` -> `src/main/java/dk/ashlan/agent/chapters/chapter04/SolveKipchogeProblemDemo.java` - Adapted port
- `structured output` -> `src/main/java/dk/ashlan/agent/chapters/chapter04/AgentStructuredOutputDemo.java` - Adapted port
- `human-in-the-loop` -> `src/main/java/dk/ashlan/agent/chapters/chapter04/HumanInTheLoopDemo.java` - Direct port
- `scratch_agents/agents/execution_context_ch4.py` -> `src/main/java/dk/ashlan/agent/core/ExecutionContext.java` - Adapted port
- `scratch_agents/agents/tool_calling_agent_ch4_*` -> `src/main/java/dk/ashlan/agent/core/AgentOrchestrator.java` and related classes - Adapted port
- `scratch_agents/types/*` -> `src/main/java/dk/ashlan/agent/types/*` - Adapted port
- `scratch_agents/types/contents.py` -> `src/main/java/dk/ashlan/agent/types/ContentItem.java`, `MessageItem.java`, `ToolCallItem.java`, `ToolResultItem.java` - Adapted port
- `scratch_agents/types/events.py` -> `src/main/java/dk/ashlan/agent/types/Event.java`, `EventType.java`, `ConversationEvent.java`, `MessageEvent.java`, `ToolCallEvent.java`, `ToolResultEvent.java`, `SystemEvent.java` - Adapted port

## `chapter_06_memory/`

- `session agent` -> `src/main/java/dk/ashlan/agent/chapters/chapter06/SessionAgentDemo.java` - Adapted port
- `core memory strategy` -> `src/main/java/dk/ashlan/agent/chapters/chapter06/CoreMemoryStrategyDemo.java` - Direct port
- `core memory update` -> `src/main/java/dk/ashlan/agent/chapters/chapter06/CoreMemoryUpdateDemo.java` - Direct port
- `sliding window` -> `src/main/java/dk/ashlan/agent/chapters/chapter06/SlidingWindowDemo.java` - Direct port
- `summarization` -> `src/main/java/dk/ashlan/agent/chapters/chapter06/SummarizationDemo.java` - Direct port
- `conversation search` -> `src/main/java/dk/ashlan/agent/chapters/chapter06/ConversationSearchDemo.java` - Direct port
- `task long-term` -> `src/main/java/dk/ashlan/agent/chapters/chapter06/TaskLongTermDemo.java` - Direct port
- `user long-term` -> `src/main/java/dk/ashlan/agent/chapters/chapter06/UserLongTermDemo.java` - Direct port
- `scratch_agents/memory/*` -> `src/main/java/dk/ashlan/agent/memory/*` - Adapted port
- `scratch_agents/sessions/*` -> `src/main/java/dk/ashlan/agent/sessions/*` - Adapted port
- `scratch_agents/sessions/base_session_manager.py` -> `src/main/java/dk/ashlan/agent/sessions/BaseSessionManager.java` - Direct port
- `scratch_agents/sessions/base_cross_session_manager.py` -> `src/main/java/dk/ashlan/agent/sessions/BaseCrossSessionManager.java` - Direct port

## Quarkus Companion Extensions

- `src/main/java/dk/ashlan/agent/rag/*`
- `src/main/java/dk/ashlan/agent/planning/*`
- `src/main/java/dk/ashlan/agent/code/*`
- `src/main/java/dk/ashlan/agent/multiagent/*`
- `src/main/java/dk/ashlan/agent/eval/*`

These modules are intentional Quarkus-native extensions that were not present in the Python zip in the same form.
