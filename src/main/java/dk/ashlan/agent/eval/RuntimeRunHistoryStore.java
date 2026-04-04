package dk.ashlan.agent.eval;

import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class RuntimeRunHistoryStore {
    private final ConcurrentHashMap<String, RuntimeRunRecord> runs = new ConcurrentHashMap<>();
    private final Deque<String> order = new ArrayDeque<>();
    private final AtomicLong sequence = new AtomicLong();

    public String nextRunId() {
        return "chapter10-run-" + sequence.incrementAndGet();
    }

    public RuntimeRunRecord record(RuntimeRunRecord record) {
        runs.put(record.runId(), record);
        synchronized (order) {
            order.remove(record.runId());
            order.addFirst(record.runId());
        }
        return record;
    }

    public List<RuntimeRunRecord> list() {
        return list(null, Integer.MAX_VALUE);
    }

    public List<RuntimeRunRecord> list(String lane, int limit) {
        int safeLimit = limit <= 0 ? Integer.MAX_VALUE : limit;
        String effectiveLane = lane == null ? "" : lane.trim();
        List<RuntimeRunRecord> records = new ArrayList<>();
        synchronized (order) {
            for (String runId : order) {
                RuntimeRunRecord record = runs.get(runId);
                if (record == null) {
                    continue;
                }
                if (!effectiveLane.isBlank() && !record.lane().equalsIgnoreCase(effectiveLane)) {
                    continue;
                }
                records.add(record);
                if (records.size() >= safeLimit) {
                    break;
                }
            }
        }
        return List.copyOf(records);
    }

    public Optional<RuntimeRunRecord> find(String runId) {
        return Optional.ofNullable(runs.get(runId));
    }
}
