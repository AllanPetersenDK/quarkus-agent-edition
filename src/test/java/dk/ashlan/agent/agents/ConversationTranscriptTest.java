package dk.ashlan.agent.agents;

import dk.ashlan.agent.types.MessageEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConversationTranscriptTest {
    @Test
    void transcriptCollectsConversationEvents() {
        ConversationTranscript transcript = new ConversationTranscript();
        transcript.add(new MessageEvent("user", "hello", Instant.EPOCH));

        assertEquals(1, transcript.events().size());
    }
}
