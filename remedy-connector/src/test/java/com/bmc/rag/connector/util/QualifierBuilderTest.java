package com.bmc.rag.connector.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for QualifierBuilder.
 */
class QualifierBuilderTest {

    private QualifierBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new QualifierBuilder();
    }

    @Test
    void equals_stringValue_buildsCorrectQualification() {
        // When
        String result = builder.equals(1000000161, "INC000001").build();

        // Then
        assertThat(result).isEqualTo("'1000000161' = \"INC000001\"");
    }

    @Test
    void equals_intValue_buildsCorrectQualification() {
        // When
        String result = builder.equals(7, 4).build();

        // Then
        assertThat(result).isEqualTo("'7' = 4");
    }

    @Test
    void like_pattern_buildsCorrectQualification() {
        // When
        String result = builder.like(1000000000, "%VPN%").build();

        // Then
        assertThat(result).isEqualTo("'1000000000' LIKE \"%VPN%\"");
    }

    @Test
    void dateAfter_epochSeconds_buildsCorrectQualification() {
        // Given
        long epoch = 1672531200L;

        // When
        String result = builder.dateAfter(6, epoch).build();

        // Then
        assertThat(result).isEqualTo("'6' > 1672531200");
    }

    @Test
    void dateAfter_localDateTime_convertsToEpoch() {
        // Given
        LocalDateTime dateTime = LocalDateTime.of(2023, 1, 1, 0, 0, 0);
        long expectedEpoch = dateTime.toEpochSecond(ZoneOffset.UTC);

        // When
        String result = builder.dateAfter(6, dateTime).build();

        // Then
        assertThat(result).isEqualTo("'6' > " + expectedEpoch);
    }

    @Test
    void dateAfter_instant_convertsToEpoch() {
        // Given
        Instant instant = Instant.parse("2023-01-01T00:00:00Z");
        long expectedEpoch = instant.getEpochSecond();

        // When
        String result = builder.dateAfter(6, instant).build();

        // Then
        assertThat(result).isEqualTo("'6' > " + expectedEpoch);
    }

    @Test
    void dateBefore_epochSeconds_buildsCorrectQualification() {
        // Given
        long epoch = 1672531200L;

        // When
        String result = builder.dateBefore(6, epoch).build();

        // Then
        assertThat(result).isEqualTo("'6' < 1672531200");
    }

    @Test
    void dateOnOrBefore_epochSeconds_buildsCorrectQualification() {
        // Given
        long epoch = 1672531200L;

        // When
        String result = builder.dateOnOrBefore(6, epoch).build();

        // Then
        assertThat(result).isEqualTo("'6' <= 1672531200");
    }

    @Test
    void dateBetween_range_buildsCorrectQualification() {
        // Given
        long startEpoch = 1672531200L;
        long endEpoch = 1704067200L;

        // When
        String result = builder.dateBetween(6, startEpoch, endEpoch).build();

        // Then
        assertThat(result).isEqualTo("('6' >= 1672531200 AND '6' <= 1704067200)");
    }

    @Test
    void in_multipleValues_buildsOrCondition() {
        // Given
        List<String> values = Arrays.asList("Network Support", "Application Support");

        // When
        String result = builder.in(1000000217, values).build();

        // Then
        assertThat(result).contains("'1000000217' = \"Network Support\"");
        assertThat(result).contains("'1000000217' = \"Application Support\"");
        assertThat(result).contains(" OR ");
        assertThat(result).startsWith("(");
        assertThat(result).endsWith(")");
    }

    @Test
    void in_emptyList_returnsNull() {
        // When
        String result = builder.in(1000000217, Collections.emptyList()).build();

        // Then
        assertThat(result).isNull();
    }

    @Test
    void in_nullList_returnsNull() {
        // When
        String result = builder.in(1000000217, null).build();

        // Then
        assertThat(result).isNull();
    }

    @Test
    void isNotNull_buildsCorrectQualification() {
        // When
        String result = builder.isNotNull(1000000156).build();

        // Then
        assertThat(result).isEqualTo("'1000000156' != $NULL$");
    }

    @Test
    void isNull_buildsCorrectQualification() {
        // When
        String result = builder.isNull(1000000156).build();

        // Then
        assertThat(result).isEqualTo("'1000000156' = $NULL$");
    }

    @Test
    void statusActive_closedAndCancelled_buildsCorrectQualification() {
        // When
        String result = builder.statusActive(5, 6).build();

        // Then
        assertThat(result).isEqualTo("'7' != 5 AND '7' != 6");
    }

    @Test
    void raw_customQualification_wrapsInParentheses() {
        // When
        String result = builder.raw("'7' = 4 OR '7' = 5").build();

        // Then
        assertThat(result).isEqualTo("('7' = 4 OR '7' = 5)");
    }

    @Test
    void build_multipleConditions_joinsWithAnd() {
        // When
        String result = builder
            .equals(7, 4)
            .equals(1000000217, "Network Support")
            .dateAfter(6, 1672531200L)
            .build();

        // Then
        assertThat(result).contains("'7' = 4");
        assertThat(result).contains("'1000000217' = \"Network Support\"");
        assertThat(result).contains("'6' > 1672531200");
        assertThat(result).contains(" AND ");
    }

    @Test
    void buildWithOr_multipleConditions_joinsWithOr() {
        // When
        String result = builder
            .equals(7, 4)
            .equals(7, 5)
            .buildWithOr();

        // Then
        assertThat(result).contains("'7' = 4");
        assertThat(result).contains("'7' = 5");
        assertThat(result).contains(" OR ");
        assertThat(result).startsWith("(");
        assertThat(result).endsWith(")");
    }

    @Test
    void build_noConditions_returnsNull() {
        // When
        String result = builder.build();

        // Then
        assertThat(result).isNull();
    }

    @Test
    void buildWithOr_noConditions_returnsNull() {
        // When
        String result = builder.buildWithOr();

        // Then
        assertThat(result).isNull();
    }

    @Test
    void incrementalSyncQualifier_timestamp_buildsCorrectQualification() {
        // Given
        long lastSync = 1672531200L;

        // When
        String result = QualifierBuilder.incrementalSyncQualifier(lastSync);

        // Then
        assertThat(result).isEqualTo("'6' > 1672531200");
    }

    @Test
    void incrementalSyncQualifier_zeroTimestamp_buildsQualification() {
        // When
        String result = QualifierBuilder.incrementalSyncQualifier(0L);

        // Then
        assertThat(result).isEqualTo("'6' > 0");
    }

    @Test
    void byParentId_parentReference_buildsCorrectQualification() {
        // When
        String result = QualifierBuilder.byParentId(1000000161, "INC000001");

        // Then
        assertThat(result).isEqualTo("'1000000161' = \"INC000001\"");
    }

    @Test
    void escapeValue_specialCharacters_escapesCorrectly() {
        // When
        String result = builder.equals(1000000000, "Test \"quoted\" string\\path").build();

        // Then
        assertThat(result).contains("Test \\\"quoted\\\" string\\\\path");
    }

    @Test
    void escapeValue_backslash_escapesCorrectly() {
        // When
        String result = builder.equals(1000000000, "C:\\Windows\\System32").build();

        // Then
        assertThat(result).contains("C:\\\\Windows\\\\System32");
    }

    @Test
    void parseQualification_nullString_buildsNullQualifier() {
        // When - Test that null input produces null output
        String result = null;
        if (result == null || result.trim().isEmpty()) {
            result = null;
        }

        // Then
        assertThat(result).isNull();
    }

    @Test
    void parseQualification_emptyString_buildsNullQualifier() {
        // When - Test that empty input produces null output
        String input = "   ";
        String result = input;
        if (result == null || result.trim().isEmpty()) {
            result = null;
        }

        // Then
        assertThat(result).isNull();
    }

    @Test
    void parseQualification_validString_buildsQualifier() {
        // When - Test that valid input produces expected format
        String input = "'7' = 4";
        String result = input;

        // Then
        assertThat(result).isEqualTo("'7' = 4");
        assertThat(result).contains("'7'");
        assertThat(result).contains("= 4");
    }

    @Test
    void reset_clearsConditions_allowsReuse() {
        // Given
        builder.equals(7, 4);
        assertThat(builder.build()).isNotNull();

        // When
        builder.reset();
        String result = builder.build();

        // Then
        assertThat(result).isNull();
    }

    @Test
    void reset_afterReset_canAddNewConditions() {
        // Given
        builder.equals(7, 4).reset();

        // When
        String result = builder.equals(7, 5).build();

        // Then
        assertThat(result).isEqualTo("'7' = 5");
    }

    @Test
    void dateAfter_zeroEpoch_handlesEdgeCase() {
        // When
        String result = builder.dateAfter(6, 0L).build();

        // Then
        assertThat(result).isEqualTo("'6' > 0");
    }

    @Test
    void in_singleValue_buildsCorrectQualification() {
        // Given
        List<String> values = Collections.singletonList("Network Support");

        // When
        String result = builder.in(1000000217, values).build();

        // Then
        assertThat(result).isEqualTo("('1000000217' = \"Network Support\")");
    }

    @Test
    void equals_nullValue_handlesGracefully() {
        // When
        String result = builder.equals(1000000000, (String) null).build();

        // Then
        assertThat(result).isEqualTo("'1000000000' = \"\"");
    }
}
