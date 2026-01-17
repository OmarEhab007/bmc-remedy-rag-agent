package com.bmc.rag.vectorization.chunking;

import com.bmc.rag.connector.model.ITSMRecord;

import java.util.List;

/**
 * Interface for record-specific chunking strategies.
 * Each ITSM record type has different chunking requirements.
 */
public interface ChunkStrategy<T extends ITSMRecord> {

    /**
     * Chunk a record into text chunks ready for embedding.
     *
     * @param record The ITSM record to chunk
     * @return List of text chunks
     */
    List<TextChunk> chunk(T record);

    /**
     * Get the record type this strategy handles.
     */
    String getRecordType();

    /**
     * Get maximum chunk size in characters.
     */
    default int getMaxChunkSize() {
        return 1000; // Default ~250 tokens for all-minilm-l6-v2
    }

    /**
     * Get chunk overlap size in characters.
     */
    default int getOverlapSize() {
        return 100;
    }
}
