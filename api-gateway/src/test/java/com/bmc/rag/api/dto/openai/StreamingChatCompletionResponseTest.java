package com.bmc.rag.api.dto.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for StreamingChatCompletionResponse.
 */
class StreamingChatCompletionResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void builder_shouldCreateInstanceWithAllFields() {
        // Given & When
        StreamingChatCompletionResponse response = StreamingChatCompletionResponse.builder()
            .id("chatcmpl-123")
            .object("chat.completion.chunk")
            .created(1234567890L)
            .model("gemini-2.0-flash")
            .choices(Collections.emptyList())
            .system_fingerprint("fp_123")
            .build();

        // Then
        assertThat(response.getId()).isEqualTo("chatcmpl-123");
        assertThat(response.getObject()).isEqualTo("chat.completion.chunk");
        assertThat(response.getCreated()).isEqualTo(1234567890L);
        assertThat(response.getModel()).isEqualTo("gemini-2.0-flash");
        assertThat(response.getSystem_fingerprint()).isEqualTo("fp_123");
    }

    @Test
    void builder_shouldUseDefaultObjectValue() {
        // Given & When
        StreamingChatCompletionResponse response = StreamingChatCompletionResponse.builder()
            .id("test")
            .build();

        // Then
        assertThat(response.getObject()).isEqualTo("chat.completion.chunk");
    }

    @Test
    void chunk_shouldCreateContentChunk() {
        // When
        StreamingChatCompletionResponse response = StreamingChatCompletionResponse.chunk(
            "chatcmpl-123",
            "gemini-2.0-flash",
            "Hello world"
        );

        // Then
        assertThat(response.getId()).isEqualTo("chatcmpl-123");
        assertThat(response.getObject()).isEqualTo("chat.completion.chunk");
        assertThat(response.getModel()).isEqualTo("gemini-2.0-flash");
        assertThat(response.getChoices()).hasSize(1);

        StreamChoice choice = response.getChoices().get(0);
        assertThat(choice.getIndex()).isEqualTo(0);
        assertThat(choice.getDelta().getContent()).isEqualTo("Hello world");
    }

    @Test
    void first_shouldCreateInitialChunkWithRole() {
        // When
        StreamingChatCompletionResponse response = StreamingChatCompletionResponse.first(
            "gemini-2.0-flash"
        );

        // Then
        assertThat(response.getId()).startsWith("chatcmpl-");
        assertThat(response.getObject()).isEqualTo("chat.completion.chunk");
        assertThat(response.getModel()).isEqualTo("gemini-2.0-flash");
        assertThat(response.getChoices()).hasSize(1);

        StreamChoice choice = response.getChoices().get(0);
        assertThat(choice.getIndex()).isEqualTo(0);
        assertThat(choice.getDelta().getRole()).isEqualTo("assistant");
    }

    @Test
    void finish_shouldCreateFinishChunk() {
        // When
        StreamingChatCompletionResponse response = StreamingChatCompletionResponse.finish(
            "chatcmpl-123",
            "gemini-2.0-flash"
        );

        // Then
        assertThat(response.getId()).isEqualTo("chatcmpl-123");
        assertThat(response.getObject()).isEqualTo("chat.completion.chunk");
        assertThat(response.getModel()).isEqualTo("gemini-2.0-flash");
        assertThat(response.getChoices()).hasSize(1);

        StreamChoice choice = response.getChoices().get(0);
        assertThat(choice.getIndex()).isEqualTo(0);
        assertThat(choice.getFinishReason()).isEqualTo("stop");
    }

    @Test
    void serializeToJson_shouldIncludeAllFields() throws Exception {
        // Given
        StreamingChatCompletionResponse response = StreamingChatCompletionResponse.builder()
            .id("test-id")
            .object("chat.completion.chunk")
            .created(1234567890L)
            .model("test-model")
            .choices(Collections.emptyList())
            .build();

        // When
        String json = objectMapper.writeValueAsString(response);

        // Then
        assertThat(json).contains("test-id");
        assertThat(json).contains("chat.completion.chunk");
        assertThat(json).contains("test-model");
    }

    @Test
    void noArgsConstructor_shouldWork() {
        // When
        StreamingChatCompletionResponse response = new StreamingChatCompletionResponse();

        // Then
        assertThat(response).isNotNull();
    }

    @Test
    void allArgsConstructor_shouldWork() {
        // When
        StreamingChatCompletionResponse response = new StreamingChatCompletionResponse(
            "id",
            "object",
            123L,
            "model",
            Collections.emptyList(),
            "fingerprint"
        );

        // Then
        assertThat(response.getId()).isEqualTo("id");
        assertThat(response.getObject()).isEqualTo("object");
        assertThat(response.getCreated()).isEqualTo(123L);
        assertThat(response.getModel()).isEqualTo("model");
        assertThat(response.getSystem_fingerprint()).isEqualTo("fingerprint");
    }

    @Test
    void equalsAndHashCode_shouldWork() {
        // Given
        StreamingChatCompletionResponse response1 = StreamingChatCompletionResponse.builder()
            .id("test")
            .model("model")
            .build();

        StreamingChatCompletionResponse response2 = StreamingChatCompletionResponse.builder()
            .id("test")
            .model("model")
            .build();

        // Then
        assertThat(response1).isEqualTo(response2);
        assertThat(response1.hashCode()).isEqualTo(response2.hashCode());
    }

    @Test
    void toString_shouldContainFields() {
        // Given
        StreamingChatCompletionResponse response = StreamingChatCompletionResponse.builder()
            .id("test-id")
            .model("test-model")
            .build();

        // When
        String toString = response.toString();

        // Then
        assertThat(toString).contains("test-id");
        assertThat(toString).contains("test-model");
    }
}
