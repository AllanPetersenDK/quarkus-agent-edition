package dk.ashlan.agent.eval.gaia;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class GaiaEvaluationStore {
    private final Map<String, GaiaRunResult> runs = new ConcurrentHashMap<>();
    private final Map<String, GaiaCaseResult> cases = new ConcurrentHashMap<>();

    public void save(GaiaRunResult runResult) {
        runs.put(runResult.runId(), runResult);
        for (GaiaCaseResult caseResult : runResult.results()) {
            cases.put(caseResult.taskId(), caseResult);
        }
    }

    public Optional<GaiaRunResult> findRun(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    public Optional<GaiaCaseResult> findCase(String taskId) {
        return Optional.ofNullable(cases.get(taskId));
    }
}
