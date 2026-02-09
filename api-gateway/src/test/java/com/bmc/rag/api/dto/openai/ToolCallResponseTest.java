package com.bmc.rag.api.dto.openai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for ToolCallResponse and nested DTOs.
 */
class ToolCallResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void builder_shouldCreateInstanceWithAllFields() {
        // Given
        ToolCallResponse.Usage usage = ToolCallResponse.Usage.builder()
            .promptTokens(100)
            .completionTokens(50)
            .totalTokens(150)
            .build();

        ToolCallResponse.ToolCallFunction function = ToolCallResponse.ToolCallFunction.builder()
            .name("create_incident")
            .arguments("{\"summary\":\"Test\"}")
            .build();

        ToolCallResponse.ToolCall toolCall = ToolCallResponse.ToolCall.builder()
            .id("call_123")
            .type("function")
            .function(function)
            .build();

        ToolCallResponse.ToolCallMessage message = ToolCallResponse.ToolCallMessage.builder()
            .role("assistant")
            .content(null)
            .toolCalls(Collections.singletonList(toolCall))
            .build();

        ToolCallResponse.ToolCallChoice choice = ToolCallResponse.ToolCallChoice.builder()
            .index(0)
            .message(message)
            .finishReason("tool_calls")
            .build();

        // When
        ToolCallResponse response = ToolCallResponse.builder()
            .id("chatcmpl-123")
            .object("chat.completion")
            .created(1234567890L)
            .model("gpt-4")
            .choices(Collections.singletonList(choice))
            .usage(usage)
            .build();

        // Then
        assertThat(response.getId()).isEqualTo("chatcmpl-123");
        assertThat(response.getObject()).isEqualTo("chat.completion");
        assertThat(response.getCreated()).isEqualTo(1234567890L);
        assertThat(response.getModel()).isEqualTo("gpt-4");
        assertThat(response.getChoices()).hasSize(1);
        assertThat(response.getUsage()).isNotNull();
        assertThat(response.getUsage().getTotalTokens()).isEqualTo(150);
    }

    @Test
    void createIncident_shouldGenerateValidResponse() {
        // When
        ToolCallResponse response = ToolCallResponse.createIncident(
            "Test summary",
            "Test description",
            "gemini-2.0-flash"
        );

        // Then
        assertThat(response.getId()).startsWith("chatcmpl-");
        assertThat(response.getObject()).isEqualTo("chat.completion");
        assertThat(response.getModel()).isEqualTo("gemini-2.0-flash");
        assertThat(response.getChoices()).hasSize(1);

        ToolCallResponse.ToolCallChoice choice = response.getChoices().get(0);
        assertThat(choice.getIndex()).isEqualTo(0);
        assertThat(choice.getFinishReason()).isEqualTo("tool_calls");
        assertThat(choice.getMessage().getRole()).isEqualTo("assistant");
        assertThat(choice.getMessage().getToolCalls()).hasSize(1);

        ToolCallResponse.ToolCall toolCall = choice.getMessage().getToolCalls().get(0);
        assertThat(toolCall.getId()).startsWith("call_");
        assertThat(toolCall.getType()).isEqualTo("function");
        assertThat(toolCall.getFunction().getName()).isEqualTo("create_incident");
        assertThat(toolCall.getFunction().getArguments()).contains("Test summary");
        assertThat(toolCall.getFunction().getArguments()).contains("Test description");
    }

    @Test
    void createIncident_shouldEscapeJsonCharacters() {
        // When
        ToolCallResponse response = ToolCallResponse.createIncident(
            "Summary with \"quotes\" and \n newlines",
            "Description with \t tabs",
            "test-model"
        );

        // Then
        ToolCallResponse.ToolCall toolCall = response.getChoices().get(0)
            .getMessage().getToolCalls().get(0);
        String args = toolCall.getFunction().getArguments();

        assertThat(args).contains("\\\"");  // Escaped quotes
        assertThat(args).contains("\\n");   // Escaped newline
        assertThat(args).contains("\\t");   // Escaped tab
    }

    @Test
    void searchIncidents_shouldGenerateValidResponse() {
        // When
        ToolCallResponse response = ToolCallResponse.searchIncidents(
            "search query",
            "gemini-2.0-flash"
        );

        // Then
        assertThat(response.getId()).startsWith("chatcmpl-");
        assertThat(response.getObject()).isEqualTo("chat.completion");
        assertThat(response.getModel()).isEqualTo("gemini-2.0-flash");
        assertThat(response.getChoices()).hasSize(1);

        ToolCallResponse.ToolCall toolCall = response.getChoices().get(0)
            .getMessage().getToolCalls().get(0);
        assertThat(toolCall.getFunction().getName()).isEqualTo("search_incidents");
        assertThat(toolCall.getFunction().getArguments()).contains("search query");
    }

    @Test
    void confirmAction_shouldGenerateValidResponse() {
        // When
        ToolCallResponse response = ToolCallResponse.confirmAction(
            "action-123",
            "gemini-2.0-flash"
        );

        // Then
        assertThat(response.getId()).startsWith("chatcmpl-");
        assertThat(response.getModel()).isEqualTo("gemini-2.0-flash");

        ToolCallResponse.ToolCall toolCall = response.getChoices().get(0)
            .getMessage().getToolCalls().get(0);
        assertThat(toolCall.getFunction().getName()).isEqualTo("confirm_action");
        assertThat(toolCall.getFunction().getArguments()).contains("action-123");
    }

    @Test
    void usage_shouldCalculateTokens() {
        // Given & When
        ToolCallResponse.Usage usage = ToolCallResponse.Usage.builder()
            .promptTokens(100)
            .completionTokens(50)
            .totalTokens(150)
            .build();

        // Then
        assertThat(usage.getPromptTokens()).isEqualTo(100);
        assertThat(usage.getCompletionTokens()).isEqualTo(50);
        assertThat(usage.getTotalTokens()).isEqualTo(150);
    }

    @Test
    void toolCallFunction_shouldSerializeToJson() throws Exception {
        // Given
        ToolCallResponse.ToolCallFunction function = ToolCallResponse.ToolCallFunction.builder()
            .name("test_function")
            .arguments("{\"param\":\"value\"}")
            .build();

        // When
        String json = objectMapper.writeValueAsString(function);

        // Then
        assertThat(json).contains("test_function");
        assertThat(json).contains("arguments");
    }

    @Test
    void toolCallChoice_equalsAndHashCode_shouldWork() {
        // Given
        ToolCallResponse.ToolCallChoice choice1 = ToolCallResponse.ToolCallChoice.builder()
            .index(0)
            .finishReason("stop")
            .build();

        ToolCallResponse.ToolCallChoice choice2 = ToolCallResponse.ToolCallChoice.builder()
            .index(0)
            .finishReason("stop")
            .build();

        // Then
        assertThat(choice1).isEqualTo(choice2);
        assertThat(choice1.hashCode()).isEqualTo(choice2.hashCode());
    }

    @Test
    void toolCallResponse_toString_shouldContainFields() {
        // Given
        ToolCallResponse response = ToolCallResponse.builder()
            .id("test-id")
            .model("test-model")
            .build();

        // When
        String toString = response.toString();

        // Then
        assertThat(toString).contains("test-id");
        assertThat(toString).contains("test-model");
    }

    @Test
    void allNestedClasses_shouldSupportNoArgsConstructor() {
        // Then - no exceptions should be thrown
        assertThat(new ToolCallResponse()).isNotNull();
        assertThat(new ToolCallResponse.ToolCallChoice()).isNotNull();
        assertThat(new ToolCallResponse.ToolCallMessage()).isNotNull();
        assertThat(new ToolCallResponse.ToolCall()).isNotNull();
        assertThat(new ToolCallResponse.ToolCallFunction()).isNotNull();
        assertThat(new ToolCallResponse.Usage()).isNotNull();
    }

    @Test
    void allNestedClasses_shouldSupportAllArgsConstructor() {
        // Then - no exceptions should be thrown
        assertThat(new ToolCallResponse("id", "obj", 123L, "model", Collections.emptyList(), null))
            .isNotNull();
        assertThat(new ToolCallResponse.ToolCallChoice(0, null, "stop"))
            .isNotNull();
        assertThat(new ToolCallResponse.ToolCallMessage("role", "content", Collections.emptyList()))
            .isNotNull();
        assertThat(new ToolCallResponse.ToolCall("id", "type", null))
            .isNotNull();
        assertThat(new ToolCallResponse.ToolCallFunction("name", "args"))
            .isNotNull();
        assertThat(new ToolCallResponse.Usage(10, 20, 30))
            .isNotNull();
    }
}
