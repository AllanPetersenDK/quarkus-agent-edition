package dk.ashlan.agent.memory;

public record MemoryExtractionResult(
        MemoryWriteDecision decision,
        TaskMemory memory,
        String reason
) {
    public static MemoryExtractionResult add(TaskMemory memory, String reason) {
        return new MemoryExtractionResult(MemoryWriteDecision.ADD, memory, reason);
    }

    public static MemoryExtractionResult skip(String reason) {
        return new MemoryExtractionResult(MemoryWriteDecision.SKIP, null, reason);
    }
}
