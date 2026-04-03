package dk.ashlan.agent.chapters.chapter07.companion;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class LangChain4jAgenticCompanionDemo {
    private final CompanionPlanWorkflow workflow;

    @Inject
    public LangChain4jAgenticCompanionDemo(Instance<CompanionPlanWorkflow> workflows) {
        this(workflows.isResolvable() ? workflows.get() : null);
    }

    public LangChain4jAgenticCompanionDemo(CompanionPlanWorkflow workflow) {
        this.workflow = workflow;
    }

    public String run(String topic) {
        if (workflow == null) {
            return "Framework-backed agentic companion demo is not configured for: " + topic;
        }
        return workflow.run(topic);
    }
}
