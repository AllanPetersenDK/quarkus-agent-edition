package dk.ashlan.agent.rag;

import jakarta.enterprise.context.ApplicationScoped;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class Chunker {
    private static final int DEFAULT_OVERLAP_CHARS = 80;

    public List<DocumentChunk> chunk(String sourceId, String text, int maxChunkSize) {
        return chunk(sourceId, text, maxChunkSize, Map.of());
    }

    public List<DocumentChunk> chunk(String sourceId, String inputText, int maxChunkSize, Map<String, String> documentMetadata) {
        String safeSourceId = normalizeSourceId(sourceId);
        String normalizedText = normalizeDocumentText(inputText);
        if (normalizedText.isBlank()) {
            return List.of();
        }
        int chunkSize = Math.max(80, maxChunkSize);
        int overlap = Math.min(DEFAULT_OVERLAP_CHARS, Math.max(16, chunkSize / 5));
        List<ChunkUnit> units = splitIntoUnits(normalizedText, chunkSize);
        if (units.isEmpty()) {
            units = List.of(new ChunkUnit(normalizedText, false));
        }

        List<DocumentChunk> chunks = new ArrayList<>();
        String carry = "";
        int chunkIndex = 0;
        for (int index = 0; index < units.size(); index++) {
            ChunkUnit unit = units.get(index);
            String text = unit.text().strip();
            if (text.isBlank()) {
                continue;
            }
            String chunkText = text;
            if (unit.overlapWithPrevious() && !carry.isBlank()) {
                String overlapCandidate = carry + "\n\n" + text;
                if (overlapCandidate.length() <= chunkSize) {
                    chunkText = overlapCandidate.strip();
                }
            }
            if (chunkText.length() > chunkSize) {
                chunkText = trimToBoundary(chunkText, chunkSize);
            }
            if (!chunkText.isBlank()) {
                chunks.add(new DocumentChunk(
                        stableChunkId(safeSourceId, chunkIndex, chunkText),
                        safeSourceId,
                        chunkIndex,
                        chunkText,
                        chunkMetadata(safeSourceId, chunkIndex, chunkText, index, index + 1, overlap, documentMetadata)
                ));
                chunkIndex++;
                carry = unit.overlapWithPrevious() ? overlapTail(chunkText, overlap) : "";
                if (carry.length() > chunkSize / 2) {
                    carry = carry.substring(Math.max(0, carry.length() - chunkSize / 2)).strip();
                }
            } else {
                carry = "";
            }
        }
        return List.copyOf(chunks);
    }

    private List<ChunkUnit> splitIntoUnits(String text, int maxChunkSize) {
        List<ChunkUnit> units = new ArrayList<>();
        for (String paragraph : text.split("\\n\\s*\\n+")) {
            String normalizedParagraph = normalizeParagraph(paragraph);
            if (normalizedParagraph.isBlank()) {
                continue;
            }
            if (normalizedParagraph.length() <= maxChunkSize) {
                units.add(new ChunkUnit(normalizedParagraph, false));
                continue;
            }
            List<String> sentences = RagTextUtils.sentences(normalizedParagraph);
            if (sentences.size() == 1 && sentences.get(0).length() > maxChunkSize) {
                units.addAll(splitHard(sentences.get(0), maxChunkSize));
                continue;
            }
            StringBuilder sentenceBuilder = new StringBuilder();
            for (String sentence : sentences) {
                if (sentenceBuilder.isEmpty()) {
                    sentenceBuilder.append(sentence.trim());
                    continue;
                }
                String candidate = sentenceBuilder + " " + sentence.trim();
                if (candidate.length() > maxChunkSize) {
                    units.add(new ChunkUnit(sentenceBuilder.toString().strip(), true));
                    sentenceBuilder.setLength(0);
                    if (sentence.length() > maxChunkSize) {
                        units.addAll(splitHard(sentence, maxChunkSize));
                    } else {
                        sentenceBuilder.append(sentence.trim());
                    }
                    continue;
                }
                sentenceBuilder.setLength(0);
                sentenceBuilder.append(candidate);
            }
            if (!sentenceBuilder.isEmpty()) {
                units.add(new ChunkUnit(sentenceBuilder.toString().strip(), true));
            }
        }
        return units;
    }

    private List<ChunkUnit> splitHard(String text, int maxChunkSize) {
        List<ChunkUnit> parts = new ArrayList<>();
        String remaining = text.trim();
        while (!remaining.isBlank()) {
            if (remaining.length() <= maxChunkSize) {
                parts.add(new ChunkUnit(remaining, true));
                break;
            }
            int split = maxChunkSize;
            while (split > 1 && !Character.isWhitespace(remaining.charAt(split - 1))) {
                split--;
            }
            if (split < maxChunkSize / 2) {
                split = maxChunkSize;
            }
            parts.add(new ChunkUnit(remaining.substring(0, split).strip(), true));
            remaining = remaining.substring(split).strip();
        }
        return parts;
    }

    private String normalizeDocumentText(String text) {
        if (text == null) {
            return "";
        }
        String canonical = text.replace("\r\n", "\n").replace('\r', '\n');
        List<String> paragraphs = new ArrayList<>();
        for (String paragraph : canonical.split("\\n\\s*\\n+")) {
            String normalizedParagraph = paragraph.lines()
                    .map(line -> line.replaceAll("\\s+", " ").strip())
                    .filter(line -> !line.isBlank())
                    .reduce((left, right) -> left + " " + right)
                    .orElse("");
            if (!normalizedParagraph.isBlank()) {
                paragraphs.add(normalizedParagraph.strip());
            }
        }
        return String.join("\n\n", paragraphs).strip();
    }

    private String normalizeParagraph(String paragraph) {
        if (paragraph == null) {
            return "";
        }
        return paragraph.replace('\n', ' ').replaceAll("\\s+", " ").strip();
    }

    private String trimToBoundary(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String normalized = text.strip();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        int split = Math.min(maxLength, normalized.length());
        while (split > 1 && !Character.isWhitespace(normalized.charAt(split - 1))) {
            split--;
        }
        if (split < maxLength / 2) {
            split = maxLength;
        }
        return normalized.substring(0, Math.min(split, normalized.length())).strip();
    }

    private String overlapTail(String text, int overlapChars) {
        if (text == null || text.isBlank() || overlapChars <= 0) {
            return "";
        }
        String normalized = text.strip();
        if (normalized.length() <= overlapChars) {
            return normalized;
        }
        String tail = normalized.substring(Math.max(0, normalized.length() - overlapChars)).strip();
        int boundary = tail.indexOf('\n');
        if (boundary >= 0 && boundary < tail.length() - 1) {
            tail = tail.substring(boundary + 1).strip();
        }
        return tail;
    }

    private Map<String, String> chunkMetadata(
            String sourceId,
            int chunkIndex,
            String chunkText,
            int unitStartIndex,
            int unitEndIndex,
            int overlapChars,
            Map<String, String> documentMetadata
    ) {
        Map<String, String> metadata = new LinkedHashMap<>();
        if (documentMetadata != null) {
            metadata.putAll(documentMetadata);
        }
        metadata.putIfAbsent("sourceId", sourceId);
        metadata.put("source", sourceId);
        metadata.put("chunkIndex", String.valueOf(chunkIndex));
        metadata.put("chunkId", stableChunkId(sourceId, chunkIndex, chunkText));
        metadata.put("chunkLength", String.valueOf(chunkText.length()));
        metadata.put("overlapChars", String.valueOf(overlapChars));
        metadata.put("segmentStart", String.valueOf(unitStartIndex));
        metadata.put("segmentEnd", String.valueOf(unitEndIndex));
        metadata.putIfAbsent("documentName", sourceId);
        metadata.putIfAbsent("fileType", metadata.getOrDefault("fileType", ""));
        metadata.putIfAbsent("contentType", metadata.getOrDefault("contentType", ""));
        metadata.putIfAbsent("documentStatus", metadata.getOrDefault("documentStatus", "TEXT_EXTRACTED"));
        metadata.putIfAbsent("sectionHint", sectionHint(chunkText));
        return metadata;
    }

    private String sectionHint(String chunkText) {
        if (chunkText == null || chunkText.isBlank()) {
            return "";
        }
        String firstLine = chunkText.lines().findFirst().orElse(chunkText).strip();
        if (firstLine.length() > 120) {
            return firstLine.substring(0, 120).strip();
        }
        return firstLine;
    }

    private String stableChunkId(String sourceId, int chunkIndex, String chunkText) {
        String input = sourceId + "|" + chunkIndex + "|" + RagTextUtils.normalize(chunkText);
        return sourceId + ":" + chunkIndex + ":" + sha256Prefix(input);
    }

    private String sha256Prefix(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < Math.min(8, hash.length); index++) {
                builder.append(String.format("%02x", hash[index]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash chunk id", exception);
        }
    }

    private String normalizeSourceId(String sourceId) {
        if (sourceId == null || sourceId.isBlank()) {
            return "document";
        }
        return sourceId.trim().replace('\\', '/');
    }

    private record ChunkUnit(String text, boolean overlapWithPrevious) {
    }
}
