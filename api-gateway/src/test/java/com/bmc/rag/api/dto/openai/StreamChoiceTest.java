package com.bmc.rag.api.dto.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for StreamChoice and Delta.
 */
class StreamChoiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void builder_shouldCreateInstanceWithAllFields() {
        // Given
        StreamChoice.Delta delta = StreamChoice.Delta.builder()
            .role("assistant")
            .content("Hello")
            .build();

        // When
        StreamChoice choice = StreamChoice.builder()
            .index(0)
            .delta(delta)
            .finishReason("stop")
            .build();

        // Then
        assertThat(choice.getIndex()).isEqualTo(0);
        assertThat(choice.getDelta()).isNotNull();
        assertThat(choice.getDelta().getRole()).isEqualTo("assistant");
        assertThat(choice.getDelta().getContent()).isEqualTo("Hello");
        assertThat(choice.getFinishReason()).isEqualTo("stop");
    }

    @Test
    void content_shouldCreateContentDelta() {
        // When
        StreamChoice choice = StreamChoice.content(0, "test content");

        // Then
        assertThat(choice.getIndex()).isEqualTo(0);
        assertThat(choice.getDelta()).isNotNull();
        assertThat(choice.getDelta().getContent()).isEqualTo("test content");
        assertThat(choice.getDelta().getRole()).isNull();
        assertThat(choice.getFinishReason()).isNull();
    }

    @Test
    void role_shouldCreateRoleDelta() {
        // When
        StreamChoice choice = StreamChoice.role(1, "assistant");

        // Then
        assertThat(choice.getIndex()).isEqualTo(1);
        assertThat(choice.getDelta()).isNotNull();
        assertThat(choice.getDelta().getRole()).isEqualTo("assistant");
        assertThat(choice.getDelta().getContent()).isNull();
        assertThat(choice.getFinishReason()).isNull();
    }

    @Test
    void finish_shouldCreateFinishDelta() {
        // When
        StreamChoice choice = StreamChoice.finish(0, "stop");

        // Then
        assertThat(choice.getIndex()).isEqualTo(0);
        assertThat(choice.getDelta()).isNotNull();
        assertThat(choice.getDelta().getRole()).isNull();
        assertThat(choice.getDelta().getContent()).isNull();
        assertThat(choice.getFinishReason()).isEqualTo("stop");
    }

    @Test
    void delta_shouldSerializeToJson() throws Exception {
        // Given
        StreamChoice.Delta delta = StreamChoice.Delta.builder()
            .role("user")
            .content("message")
            .build();

        // When
        String json = objectMapper.writeValueAsString(delta);

        // Then
        assertThat(json).contains("user");
        assertThat(json).contains("message");
    }

    @Test
    void noArgsConstructor_shouldWork() {
        // When & Then
        assertThat(new StreamChoice()).isNotNull();
        assertThat(new StreamChoice.Delta()).isNotNull();
    }

    @Test
    void allArgsConstructor_shouldWork() {
        // Given
        StreamChoice.Delta delta = new StreamChoice.Delta("role", "content");

        // When
        StreamChoice choice = new StreamChoice(0, delta, "stop");

        // Then
        assertThat(choice.getIndex()).isEqualTo(0);
        assertThat(choice.getDelta()).isEqualTo(delta);
        assertThat(choice.getFinishReason()).isEqualTo("stop");
    }

    @Test
    void equalsAndHashCode_shouldWork() {
        // Given
        StreamChoice choice1 = StreamChoice.builder()
            .index(0)
            .finishReason("stop")
            .build();

        StreamChoice choice2 = StreamChoice.builder()
            .index(0)
            .finishReason("stop")
            .build();

        // Then
        assertThat(choice1).isEqualTo(choice2);
        assertThat(choice1.hashCode()).isEqualTo(choice2.hashCode());
    }

    @Test
    void toString_shouldContainFields() {
        // Given
        StreamChoice choice = StreamChoice.builder()
            .index(0)
            .finishReason("stop")
            .build();

        // When
        String toString = choice.toString();

        // Then
        assertThat(toString).contains("index");
        assertThat(toString).contains("stop");
    }
}
