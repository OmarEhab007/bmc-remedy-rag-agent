package com.bmc.rag.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * OkHttp interceptor that adds thinking mode parameters to Z.AI API requests.
 * This enables the GLM-4.7 thinking mode for enhanced reasoning capabilities.
 *
 * When thinking mode is enabled, the LLM will show its reasoning process,
 * which can be useful for debugging and understanding complex responses.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "zai.enabled", havingValue = "true", matchIfMissing = false)
public class ZaiRequestInterceptor implements Interceptor {

    private final ZaiConfig zaiConfig;
    private final ObjectMapper mapper;

    public ZaiRequestInterceptor(ZaiConfig zaiConfig) {
        this.zaiConfig = zaiConfig;
        this.mapper = new ObjectMapper();
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        // Only modify chat completion requests
        if (!request.url().encodedPath().contains("/chat/completions")) {
            return chain.proceed(request);
        }

        // Add thinking parameter to request body
        RequestBody originalBody = request.body();
        if (originalBody == null) {
            return chain.proceed(request);
        }

        try {
            Buffer buffer = new Buffer();
            originalBody.writeTo(buffer);
            String json = buffer.readUtf8();

            ObjectNode node = (ObjectNode) mapper.readTree(json);

            // Add thinking configuration - disable if not enabled to prevent reasoning_content
            ObjectNode thinking = mapper.createObjectNode();
            if (zaiConfig.isThinkingEnabled()) {
                thinking.put("type", zaiConfig.getThinkingType());
                log.debug("Added thinking mode to Z.AI request: type={}", zaiConfig.getThinkingType());
            } else {
                // Explicitly disable thinking to prevent reasoning_content in streaming
                thinking.put("type", "disabled");
                log.debug("Disabled thinking mode in Z.AI request to prevent reasoning_content");
            }
            node.set("thinking", thinking);

            String modifiedJson = mapper.writeValueAsString(node);

            RequestBody newBody = RequestBody.create(
                modifiedJson,
                MediaType.parse("application/json")
            );

            request = request.newBuilder()
                .post(newBody)
                .header("Content-Length", String.valueOf(modifiedJson.length()))
                .build();

        } catch (Exception e) {
            log.warn("Failed to modify thinking mode in request: {}", e.getMessage());
            // Fall back to original request if modification fails
            return chain.proceed(request);
        }

        return chain.proceed(request);
    }
}
