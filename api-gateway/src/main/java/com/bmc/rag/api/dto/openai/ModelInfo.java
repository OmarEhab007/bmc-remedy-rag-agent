package com.bmc.rag.api.dto.openai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * OpenAI-compatible model information DTO.
 * See: https://platform.openai.com/docs/api-reference/models/object
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModelInfo {

    /**
     * The model identifier.
     */
    private String id;

    /**
     * The object type, always "model".
     */
    @Builder.Default
    private String object = "model";

    /**
     * Unix timestamp of when the model was created.
     */
    private long created;

    /**
     * The organization that owns the model.
     */
    @JsonProperty("owned_by")
    private String ownedBy;

    /**
     * Create the BMC Remedy RAG model info.
     */
    public static ModelInfo bmcRemedyRag() {
        return ModelInfo.builder()
            .id("bmc-remedy-rag")
            .object("model")
            .created(1700000000L) // Fixed timestamp for consistency
            .ownedBy("bmc-rag-system")
            .build();
    }
}
