package com.bmc.rag.api.controller;

import com.bmc.rag.api.config.RateLimitConfig;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for HealthController.
 */
@WebMvcTest(
    controllers = HealthController.class,
    excludeAutoConfiguration = OAuth2ResourceServerAutoConfiguration.class
)
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private ThreadLocalARContext threadLocalARContext;

    @MockBean
    private RateLimitConfig rateLimitConfig;

    @Test
    void health_databaseUp_returnsHealthy() throws Exception {
        // Given
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        when(jdbcTemplate.queryForObject(anyString(), eq(Boolean.class))).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.database").value("UP"))
            .andExpect(jsonPath("$.pgvector").value("UP"));
    }

    @Test
    void health_databaseDown_returnsUnhealthy() throws Exception {
        // Given
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(get("/api/v1/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.database").value("DOWN"))
            .andExpect(jsonPath("$.databaseError").exists());
    }

    @Test
    void ready_databaseUp_returnsReady() throws Exception {
        // Given
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        // When & Then
        mockMvc.perform(get("/api/v1/ready"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ready"));
    }

    @Test
    void ready_databaseDown_returnsNotReady() throws Exception {
        // Given
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(get("/api/v1/ready"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.status").value("not ready"));
    }

    @Test
    void live_returnsAlive() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/v1/live"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("alive"));
    }
}
