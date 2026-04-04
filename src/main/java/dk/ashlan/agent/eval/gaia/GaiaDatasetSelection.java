package dk.ashlan.agent.eval.gaia;

public record GaiaDatasetSelection(
        String datasetUrl,
        String localPath,
        String config,
        String split,
        Integer level,
        Integer limit,
        boolean failFast
) {
    public int effectiveLimit(int defaultLimit) {
        return limit == null || limit < 1 ? defaultLimit : limit;
    }

    public String resolvedSource() {
        if (localPath != null && !localPath.isBlank()) {
            return localPath.trim();
        }
        if (datasetUrl != null && !datasetUrl.isBlank()) {
            return datasetUrl.trim();
        }
        return "";
    }
}
