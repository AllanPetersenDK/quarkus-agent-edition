package dk.ashlan.agent.eval.gaia;

import java.util.List;

/**
 * Legacy starter-harness filter for the older GAIA chapter-10 evaluation path.
 * The canonical validation flow now lives in {@link GaiaValidationRunner}.
 */
@Deprecated(forRemoval = false)
final class GaiaCaseFilter {
    private GaiaCaseFilter() {
    }

    static List<GaiaValidationCase> level1WithoutAttachments(List<GaiaValidationCase> cases) {
        return cases.stream()
                .filter(GaiaCaseFilter::isLevel1WithoutAttachment)
                .toList();
    }

    static boolean isLevel1WithoutAttachment(GaiaValidationCase validationCase) {
        return isLevel1(validationCase.level()) && hasNoAttachment(validationCase.filePath());
    }

    private static boolean isLevel1(String level) {
        return extractDigits(level).equals("1");
    }

    private static boolean hasNoAttachment(String filePath) {
        if (filePath == null) {
            return true;
        }
        String cleaned = filePath.trim();
        if (cleaned.isBlank() || cleaned.equalsIgnoreCase("null")) {
            return true;
        }
        if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
            String inner = cleaned.substring(1, cleaned.length() - 1).trim();
            return inner.isBlank() || inner.equalsIgnoreCase("null");
        }
        return false;
    }

    private static String extractDigits(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[^0-9]", "");
    }
}
