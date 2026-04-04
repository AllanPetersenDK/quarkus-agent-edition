package dk.ashlan.agent.multiagent;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Comparator;

@ApplicationScoped
public class Chapter9RunHistoryStore {
    private final AtomicLong sequence = new AtomicLong();
    private final LinkedHashMap<String, AgentTaskResult> runs = new LinkedHashMap<>();

    public synchronized String nextRunId() {
        return "chapter9-run-" + sequence.incrementAndGet();
    }

    public synchronized AgentTaskResult record(AgentTaskResult run) {
        runs.put(run.runId(), run);
        return run;
    }

    public synchronized List<AgentTaskResult> list() {
        ArrayList<AgentTaskResult> copy = new ArrayList<>(runs.values());
        copy.sort(Comparator.comparingLong(this::runNumber).reversed());
        return List.copyOf(copy);
    }

    public synchronized Optional<AgentTaskResult> find(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }

    private long runNumber(AgentTaskResult run) {
        String runId = run.runId();
        if (runId == null) {
            return -1L;
        }
        int index = runId.lastIndexOf('-');
        if (index < 0 || index == runId.length() - 1) {
            return -1L;
        }
        try {
            return Long.parseLong(runId.substring(index + 1));
        } catch (NumberFormatException ignored) {
            return -1L;
        }
    }
}
