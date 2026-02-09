package com.bmc.rag.connector.service;

import com.bmc.rag.connector.connection.ThreadLocalARContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RemedyUserService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RemedyUserServiceTest {

    @Mock
    private ThreadLocalARContext mockArContext;

    private RemedyUserService remedyUserService;

    @BeforeEach
    void setUp() {
        remedyUserService = new RemedyUserService(mockArContext);
    }

    @Test
    void getUserGroups_validUser_returnsGroups() {
        // Given
        String loginId = "testuser";
        Set<String> expectedGroups = Set.of("Network Support", "Application Support");

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedGroups);

        // When
        Set<String> groups = remedyUserService.getUserGroups(loginId);

        // Then
        assertThat(groups).contains("Network Support", "Application Support");
    }

    @Test
    void getUserGroups_cachedUser_returnsCachedGroups() {
        // Given
        String loginId = "testuser";
        Set<String> expectedGroups = Set.of("Network Support");

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedGroups);

        // First call - should query Remedy
        Set<String> groups1 = remedyUserService.getUserGroups(loginId);

        // Second call - should use cache
        Set<String> groups2 = remedyUserService.getUserGroups(loginId);

        // Then
        assertThat(groups1).isEqualTo(groups2);
        verify(mockArContext, times(1)).executeWithRetry(any());
    }

    @Test
    void getUserGroups_nullLoginId_returnsEmptySet() {
        // When
        Set<String> groups = remedyUserService.getUserGroups(null);

        // Then
        assertThat(groups).isEmpty();
    }

    @Test
    void getUserGroups_blankLoginId_returnsEmptySet() {
        // When
        Set<String> groups = remedyUserService.getUserGroups("   ");

        // Then
        assertThat(groups).isEmpty();
    }

    @Test
    void hasSubmitterRole_validSubmitter_returnsTrue() {
        // Given
        String loginId = "submitter";
        when(mockArContext.executeWithRetry(any())).thenReturn(true);

        // When
        boolean result = remedyUserService.hasSubmitterRole(loginId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasSubmitterRole_notSubmitter_returnsFalse() {
        // Given
        String loginId = "fixeduser";
        when(mockArContext.executeWithRetry(any())).thenReturn(false);

        // When
        boolean result = remedyUserService.hasSubmitterRole(loginId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getUserInfo_validUser_returnsInfo() {
        // Given
        String loginId = "testuser";
        RemedyUserService.UserInfo expectedInfo = new RemedyUserService.UserInfo(
            loginId,
            "PERSON123",
            RemedyUserService.LICENSE_FIXED,
            Set.of("Network Support")
        );

        when(mockArContext.executeWithRetry(any())).thenReturn(expectedInfo);

        // When
        RemedyUserService.UserInfo userInfo = remedyUserService.getUserInfo(loginId);

        // Then
        assertThat(userInfo).isNotNull();
        assertThat(userInfo.loginId()).isEqualTo(loginId);
    }

    @Test
    void clearCache_removesAllEntries() {
        // When
        remedyUserService.clearCache();

        // Then - Should not throw
        assertThat(remedyUserService.getCacheStats().size()).isZero();
    }

    @Test
    void getCacheStats_returnsStats() {
        // When
        RemedyUserService.CacheStats stats = remedyUserService.getCacheStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.size()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void userInfo_licenseTypeLabels_returnCorrectLabels() {
        // Given
        RemedyUserService.UserInfo fixed = new RemedyUserService.UserInfo("user", "P1", RemedyUserService.LICENSE_FIXED, Set.of());
        RemedyUserService.UserInfo floating = new RemedyUserService.UserInfo("user", "P1", RemedyUserService.LICENSE_FLOATING, Set.of());
        RemedyUserService.UserInfo read = new RemedyUserService.UserInfo("user", "P1", RemedyUserService.LICENSE_READ, Set.of());
        RemedyUserService.UserInfo submitter = new RemedyUserService.UserInfo("user", "P1", RemedyUserService.LICENSE_SUBMITTER, Set.of());

        // Then
        assertThat(fixed.getLicenseTypeName()).isEqualTo("Fixed");
        assertThat(floating.getLicenseTypeName()).isEqualTo("Floating");
        assertThat(read.getLicenseTypeName()).isEqualTo("Read");
        assertThat(submitter.getLicenseTypeName()).isEqualTo("Submitter");
        assertThat(submitter.isSubmitter()).isTrue();
        assertThat(fixed.isFixed()).isTrue();
        assertThat(floating.isFloating()).isTrue();
    }

    @Test
    void hasSubmitterRole_nullLoginId_returnsFalse() {
        // When
        boolean result = remedyUserService.hasSubmitterRole(null);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasSubmitterRole_blankLoginId_returnsFalse() {
        // When
        boolean result = remedyUserService.hasSubmitterRole("  ");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void hasSubmitterRole_exceptionThrown_returnsFalse() {
        // Given
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new RuntimeException("Query error"));

        // When
        boolean result = remedyUserService.hasSubmitterRole("erroruser");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getUserInfo_nullLoginId_returnsNull() {
        // When
        RemedyUserService.UserInfo result = remedyUserService.getUserInfo(null);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void getUserInfo_blankLoginId_returnsNull() {
        // When
        RemedyUserService.UserInfo result = remedyUserService.getUserInfo("  ");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void getUserInfo_exceptionThrown_returnsNull() {
        // Given
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new RuntimeException("Query error"));

        // When
        RemedyUserService.UserInfo result = remedyUserService.getUserInfo("erroruser");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void userInfo_unknownLicenseType_returnsUnknown() {
        // Given
        RemedyUserService.UserInfo unknown = new RemedyUserService.UserInfo("user", "P1", 999, Set.of());

        // Then
        assertThat(unknown.getLicenseTypeName()).isEqualTo("Unknown");
    }

    @Test
    void getUserGroups_exceptionDuringFetch_returnsEmptySet() {
        // Given
        when(mockArContext.executeWithRetry(any()))
            .thenThrow(new RuntimeException("Database error"));

        // When
        Set<String> groups = remedyUserService.getUserGroups("erroruser");

        // Then
        assertThat(groups).isEmpty();
    }

    @Test
    void getUserInfo_userNotFound_returnsNull() {
        // Given
        when(mockArContext.executeWithRetry(any())).thenReturn(null);

        // When
        RemedyUserService.UserInfo result = remedyUserService.getUserInfo("nonexistent");

        // Then
        assertThat(result).isNull();
    }

    @Test
    void cacheStats_afterSomeHits_returnsAccurateStats() {
        // Given
        Set<String> groups = Set.of("Group1", "Group2");
        when(mockArContext.executeWithRetry(any())).thenReturn(groups);

        // First call - miss
        remedyUserService.getUserGroups("user1");
        // Second call - hit
        remedyUserService.getUserGroups("user1");

        // When
        RemedyUserService.CacheStats stats = remedyUserService.getCacheStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.size()).isGreaterThan(0);
        assertThat(stats.hitCount()).isGreaterThan(0);
    }

    @Test
    void clearCache_afterMultipleEntries_clearsAll() {
        // Given
        Set<String> groups = Set.of("Group1");
        when(mockArContext.executeWithRetry(any())).thenReturn(groups);
        remedyUserService.getUserGroups("user1");
        remedyUserService.getUserGroups("user2");

        // When
        remedyUserService.clearCache();

        // Then
        RemedyUserService.CacheStats stats = remedyUserService.getCacheStats();
        assertThat(stats.size()).isZero();
    }

    @Test
    void userInfo_allLicenseTypeMethods_returnCorrectValues() {
        // Test all license type helper methods
        RemedyUserService.UserInfo submitter = new RemedyUserService.UserInfo(
            "user", "P1", RemedyUserService.LICENSE_SUBMITTER, Set.of());
        RemedyUserService.UserInfo fixed = new RemedyUserService.UserInfo(
            "user", "P1", RemedyUserService.LICENSE_FIXED, Set.of());
        RemedyUserService.UserInfo floating = new RemedyUserService.UserInfo(
            "user", "P1", RemedyUserService.LICENSE_FLOATING, Set.of());
        RemedyUserService.UserInfo read = new RemedyUserService.UserInfo(
            "user", "P1", RemedyUserService.LICENSE_READ, Set.of());

        // Submitter checks
        assertThat(submitter.isSubmitter()).isTrue();
        assertThat(submitter.isFixed()).isFalse();
        assertThat(submitter.isFloating()).isFalse();

        // Fixed checks
        assertThat(fixed.isFixed()).isTrue();
        assertThat(fixed.isSubmitter()).isFalse();
        assertThat(fixed.isFloating()).isFalse();

        // Floating checks
        assertThat(floating.isFloating()).isTrue();
        assertThat(floating.isFixed()).isFalse();
        assertThat(floating.isSubmitter()).isFalse();

        // Read has no specific checker, so all should be false
        assertThat(read.isSubmitter()).isFalse();
        assertThat(read.isFixed()).isFalse();
        assertThat(read.isFloating()).isFalse();
    }

    @Test
    void getUserGroups_emptyLoginId_returnsEmptySet() {
        // When
        Set<String> groups = remedyUserService.getUserGroups("");

        // Then
        assertThat(groups).isEmpty();
    }

    @Test
    void hasSubmitterRole_emptyLoginId_returnsFalse() {
        // When
        boolean result = remedyUserService.hasSubmitterRole("");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void getUserInfo_emptyLoginId_returnsNull() {
        // When
        RemedyUserService.UserInfo result = remedyUserService.getUserInfo("");

        // Then
        assertThat(result).isNull();
    }
}
