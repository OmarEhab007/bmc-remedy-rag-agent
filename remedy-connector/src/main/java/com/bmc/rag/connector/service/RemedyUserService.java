package com.bmc.rag.connector.service;

import com.bmc.arsys.api.*;
import com.bmc.rag.connector.connection.ThreadLocalARContext;
import com.bmc.rag.connector.util.QualifierBuilder;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

/**
 * Service for retrieving user information from BMC Remedy.
 * Provides user group memberships for ReBAC filtering.
 */
@Slf4j
@Service
public class RemedyUserService {

    private final ThreadLocalARContext arContext;

    // Cache user groups for 5 minutes to reduce Remedy queries
    private final Cache<String, Set<String>> userGroupsCache;

    // Remedy form and field constants
    private static final String PEOPLE_FORM = "CTM:People";
    private static final int LOGIN_ID = 4;              // Login ID field
    private static final int SUPPORT_GROUPS = 1000000034;  // Support Groups field (multi-value)
    private static final int LICENSE_TYPE = 1000000012;    // License Type
    private static final int PERSON_ID = 1000000171;       // Person ID

    // License type values
    public static final int LICENSE_FIXED = 1;
    public static final int LICENSE_FLOATING = 2;
    public static final int LICENSE_READ = 3;
    public static final int LICENSE_SUBMITTER = 4;

    public RemedyUserService(ThreadLocalARContext arContext) {
        this.arContext = arContext;
        this.userGroupsCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(5))
            .maximumSize(10_000)
            .recordStats()
            .build();
    }

    /**
     * Get the support groups for a user by login ID.
     *
     * @param loginId The user's login ID
     * @return Set of group names the user belongs to
     */
    public Set<String> getUserGroups(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return Collections.emptySet();
        }

        // Check cache first
        Set<String> cached = userGroupsCache.getIfPresent(loginId);
        if (cached != null) {
            log.debug("Cache hit for user groups: {}", loginId);
            return cached;
        }

        // Query Remedy
        try {
            Set<String> groups = fetchUserGroupsFromRemedy(loginId);
            userGroupsCache.put(loginId, groups);
            log.info("Fetched {} groups for user {}", groups.size(), loginId);
            return groups;
        } catch (Exception e) {
            log.error("Failed to fetch groups for user {}: {}", loginId, e.getMessage(), e);
            return Collections.emptySet();
        }
    }

    /**
     * Check if a user has the Submitter license type.
     *
     * @param loginId The user's login ID
     * @return true if user has Submitter license
     */
    public boolean hasSubmitterRole(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return false;
        }

        try {
            return arContext.executeWithRetry(ctx -> {
                String qualification = String.format("'%d' = \"%s\"", LOGIN_ID, escapeValue(loginId));
                QualifierInfo qualifier = QualifierBuilder.parseQualification(ctx, PEOPLE_FORM, qualification);

                List<Entry> entries = ctx.getListEntryObjects(
                    PEOPLE_FORM,
                    qualifier,
                    0, 1,
                    null,
                    new int[]{ LICENSE_TYPE },
                    false, null
                );

                if (entries != null && !entries.isEmpty()) {
                    Value licenseValue = entries.get(0).get(LICENSE_TYPE);
                    if (licenseValue != null) {
                        return ((Number) licenseValue.getValue()).intValue() == LICENSE_SUBMITTER;
                    }
                }
                return false;
            });
        } catch (Exception e) {
            log.error("Failed to check submitter role for {}: {}", loginId, e.getMessage());
            return false;
        }
    }

    /**
     * Get user information including person ID and license type.
     *
     * @param loginId The user's login ID
     * @return UserInfo record or null if not found
     */
    public UserInfo getUserInfo(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            return null;
        }

        try {
            return arContext.executeWithRetry(ctx -> {
                String qualification = String.format("'%d' = \"%s\"", LOGIN_ID, escapeValue(loginId));
                QualifierInfo qualifier = QualifierBuilder.parseQualification(ctx, PEOPLE_FORM, qualification);

                List<Entry> entries = ctx.getListEntryObjects(
                    PEOPLE_FORM,
                    qualifier,
                    0, 1,
                    null,
                    new int[]{ PERSON_ID, LICENSE_TYPE, SUPPORT_GROUPS },
                    false, null
                );

                if (entries != null && !entries.isEmpty()) {
                    Entry entry = entries.get(0);
                    String personId = getStringValue(entry, PERSON_ID);
                    int licenseType = getIntValue(entry, LICENSE_TYPE);
                    Set<String> groups = parseMultiValue(entry, SUPPORT_GROUPS);

                    return new UserInfo(loginId, personId, licenseType, groups);
                }
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to get user info for {}: {}", loginId, e.getMessage());
            return null;
        }
    }

    /**
     * Fetch user groups from Remedy CTM:People form.
     */
    private Set<String> fetchUserGroupsFromRemedy(String loginId) throws Exception {
        return arContext.executeWithRetry(ctx -> {
            String qualification = String.format("'%d' = \"%s\"", LOGIN_ID, escapeValue(loginId));
            QualifierInfo qualifier = QualifierBuilder.parseQualification(ctx, PEOPLE_FORM, qualification);

            List<Entry> entries = ctx.getListEntryObjects(
                PEOPLE_FORM,
                qualifier,
                0, 1,
                null,
                new int[]{ SUPPORT_GROUPS },
                false, null
            );

            if (entries != null && !entries.isEmpty()) {
                return parseMultiValue(entries.get(0), SUPPORT_GROUPS);
            }
            return Collections.<String>emptySet();
        });
    }

    /**
     * Parse a multi-value field (semicolon or newline separated).
     */
    private Set<String> parseMultiValue(Entry entry, int fieldId) {
        Value value = entry.get(fieldId);
        if (value == null || value.getValue() == null) {
            return Collections.emptySet();
        }

        String stringValue = value.toString();
        if (stringValue.isBlank()) {
            return Collections.emptySet();
        }

        // Multi-value fields in Remedy are often semicolon or newline separated
        String[] parts = stringValue.split("[;\n\r]+");
        Set<String> result = new HashSet<>();
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    /**
     * Get string value from entry.
     */
    private String getStringValue(Entry entry, int fieldId) {
        Value value = entry.get(fieldId);
        return value != null ? value.toString() : null;
    }

    /**
     * Get integer value from entry.
     */
    private int getIntValue(Entry entry, int fieldId) {
        Value value = entry.get(fieldId);
        if (value != null && value.getValue() != null) {
            return ((Number) value.getValue()).intValue();
        }
        return 0;
    }

    /**
     * Escape special characters in values for Remedy qualifications.
     */
    private String escapeValue(String value) {
        if (value == null) return "";
        // Escape double quotes and backslashes
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Clear the user groups cache (for admin use).
     */
    public void clearCache() {
        userGroupsCache.invalidateAll();
        log.info("User groups cache cleared");
    }

    /**
     * Get cache statistics.
     */
    public CacheStats getCacheStats() {
        var stats = userGroupsCache.stats();
        return new CacheStats(
            userGroupsCache.estimatedSize(),
            stats.hitCount(),
            stats.missCount(),
            stats.hitRate()
        );
    }

    /**
     * User information record.
     */
    public record UserInfo(
        String loginId,
        String personId,
        int licenseType,
        Set<String> groups
    ) {
        public boolean isSubmitter() {
            return licenseType == LICENSE_SUBMITTER;
        }

        public boolean isFixed() {
            return licenseType == LICENSE_FIXED;
        }

        public boolean isFloating() {
            return licenseType == LICENSE_FLOATING;
        }

        public String getLicenseTypeName() {
            return switch (licenseType) {
                case LICENSE_FIXED -> "Fixed";
                case LICENSE_FLOATING -> "Floating";
                case LICENSE_READ -> "Read";
                case LICENSE_SUBMITTER -> "Submitter";
                default -> "Unknown";
            };
        }
    }

    /**
     * Cache statistics record.
     */
    public record CacheStats(
        long size,
        long hitCount,
        long missCount,
        double hitRate
    ) {}
}
