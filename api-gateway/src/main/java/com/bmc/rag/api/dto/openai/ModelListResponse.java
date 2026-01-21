package com.bmc.rag.api.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * OpenAI-compatible model list response DTO.
 * See: https://platform.openai.com/docs/api-reference/models/list
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelListResponse {

    /**
     * The object type, always "list".
     */
    @Builder.Default
    private String object = "list";

    /**
     * The list of available models.
     */
    private List<ModelInfo> data;

    /**
     * Create the default model list for BMC RAG.
     */
    public static ModelListResponse defaultList() {
        return ModelListResponse.builder()
            .object("list")
            .data(Collections.singletonList(ModelInfo.bmcRemedyRag()))
            .build();
    }
}
