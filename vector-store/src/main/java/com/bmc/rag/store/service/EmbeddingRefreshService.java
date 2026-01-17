package com.bmc.rag.store.service;

import com.bmc.rag.store.entity.EmbeddingEntity;
import com.bmc.rag.store.repository.EmbeddingRepository;
import com.bmc.rag.vectorization.embedding.LocalEmbeddingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service to refresh embeddings in the database using real semantic embeddings.
 * This is used to fix data that was inserted with random embeddings (e.g., dummy/test data).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingRefreshService {

    private final EmbeddingRepository repository;
    private final LocalEmbeddingService embeddingService;

    // Page size for batch processing to prevent OOM
    private static final int PAGE_SIZE = 100;

    /**
     * Refresh all embeddings in the database by re-embedding the text segments.
     * Uses pagination to prevent OOM with large datasets.
     *
     * @return The number of embeddings refreshed
     */
    public int refreshAllEmbeddings() {
        log.info("Starting paginated embedding refresh for all records...");

        int page = 0;
        int totalRefreshed = 0;
        int totalFailed = 0;
        Page<EmbeddingEntity> entityPage;

        do {
            // Fetch one page at a time to prevent OOM
            entityPage = repository.findAll(PageRequest.of(page, PAGE_SIZE));
            int pageTotal = entityPage.getNumberOfElements();
            log.info("Processing page {} ({} records)", page, pageTotal);

            for (EmbeddingEntity entity : entityPage) {
                try {
                    refreshSingleEmbedding(entity);
                    totalRefreshed++;

                    if (totalRefreshed % 50 == 0) {
                        log.info("Progress: refreshed {} embeddings so far", totalRefreshed);
                    }
                } catch (Exception e) {
                    totalFailed++;
                    log.error("Failed to refresh embedding for entity {}: {}", entity.getId(), e.getMessage());
                }
            }

            page++;
        } while (entityPage.hasNext());

        log.info("Embedding refresh completed. Refreshed: {}, Failed: {}", totalRefreshed, totalFailed);
        return totalRefreshed;
    }

    /**
     * Refresh a single embedding with its own transaction.
     */
    @Transactional
    protected void refreshSingleEmbedding(EmbeddingEntity entity) {
        float[] embedding = embeddingService.embed(entity.getTextSegment());
        repository.updateEmbedding(entity.getId(), formatEmbedding(embedding));
    }

    /**
     * Refresh embeddings for a specific source type.
     * Uses pagination to prevent OOM with large datasets.
     *
     * @param sourceType The source type to refresh (e.g., "Incident", "KnowledgeArticle")
     * @return The number of embeddings refreshed
     */
    public int refreshEmbeddingsBySourceType(String sourceType) {
        log.info("Starting paginated embedding refresh for source type: {}", sourceType);

        int page = 0;
        int totalRefreshed = 0;
        int totalFailed = 0;
        Page<EmbeddingEntity> entityPage;

        do {
            entityPage = repository.findBySourceType(sourceType, PageRequest.of(page, PAGE_SIZE));
            log.info("Processing page {} for {} ({} records)", page, sourceType, entityPage.getNumberOfElements());

            for (EmbeddingEntity entity : entityPage) {
                try {
                    refreshSingleEmbedding(entity);
                    totalRefreshed++;
                } catch (Exception e) {
                    totalFailed++;
                    log.error("Failed to refresh embedding for entity {}: {}", entity.getId(), e.getMessage());
                }
            }

            page++;
        } while (entityPage.hasNext());

        log.info("Embedding refresh for {} completed. Refreshed: {}, Failed: {}", sourceType, totalRefreshed, totalFailed);
        return totalRefreshed;
    }

    /**
     * Format embedding array as PostgreSQL vector string.
     */
    private String formatEmbedding(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
