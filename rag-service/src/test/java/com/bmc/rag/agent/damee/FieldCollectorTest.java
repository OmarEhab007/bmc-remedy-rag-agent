package com.bmc.rag.agent.damee;

import com.bmc.rag.agent.damee.FieldCollector.FieldCollectionResult;
import com.bmc.rag.agent.damee.ServiceFieldDefinition.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link FieldCollector}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FieldCollectorTest {

    @InjectMocks
    private FieldCollector fieldCollector;

    @Test
    void getFieldsForService_knownService_returnsSpecificFields() {
        // When: get fields for VPN service
        List<ServiceFieldDefinition> fields = fieldCollector.getFieldsForService("10513");

        // Then: returns VPN-specific fields
        assertThat(fields).isNotEmpty();
        assertThat(fields).anyMatch(f -> "vpnType".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "justification".equals(f.getFieldName()));
    }

    @Test
    void getFieldsForService_unknownService_returnsDefaultFields() {
        // When: get fields for unknown service
        List<ServiceFieldDefinition> fields = fieldCollector.getFieldsForService("99999");

        // Then: returns default fields
        assertThat(fields).isNotEmpty();
        assertThat(fields).anyMatch(f -> "description".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "justification".equals(f.getFieldName()));
    }

    @Test
    void getFieldsForService_applicationsPermission_hasCorrectFields() {
        // When: get fields for service 10504
        List<ServiceFieldDefinition> fields = fieldCollector.getFieldsForService("10504");

        // Then: has application permission fields
        assertThat(fields).hasSize(3);
        assertThat(fields).anyMatch(f -> "serviceOption".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "applicationName".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "justification".equals(f.getFieldName()));
    }

    @Test
    void getFieldsForService_softwareInstallation_hasCorrectFields() {
        // When: get fields for service 10247
        List<ServiceFieldDefinition> fields = fieldCollector.getFieldsForService("10247");

        // Then: has software installation fields
        assertThat(fields).anyMatch(f -> "softwareName".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "isCertified".equals(f.getFieldName()));
    }

    @Test
    void getFieldsForService_serverAccess_hasEnvironmentField() {
        // When: get fields for service 10209
        List<ServiceFieldDefinition> fields = fieldCollector.getFieldsForService("10209");

        // Then: has environment field
        assertThat(fields).anyMatch(f -> "environment".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "serverName".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "accessType".equals(f.getFieldName()));
    }

    @Test
    void collectNextField_noFieldsCollected_returnsFirstRequiredField() {
        // Given: no fields collected
        Map<String, String> collectedFields = new HashMap<>();

        // When: collect next field for VPN service
        FieldCollectionResult result = fieldCollector.collectNextField("10513", collectedFields);

        // Then: returns first required field
        assertThat(result.isNeedsInput()).isTrue();
        assertThat(result.isComplete()).isFalse();
        assertThat(result.getField()).isNotNull();
        assertThat(result.getField().isRequired()).isTrue();
    }

    @Test
    void collectNextField_someFieldsCollected_returnsNextField() {
        // Given: first field collected
        Map<String, String> collectedFields = new HashMap<>();
        collectedFields.put("vpnType", "Add User to VPN Client");

        // When: collect next field
        FieldCollectionResult result = fieldCollector.collectNextField("10513", collectedFields);

        // Then: returns next required field
        assertThat(result.isNeedsInput()).isTrue();
        assertThat(result.getField()).isNotNull();
        assertThat(result.getField().getFieldName()).isNotEqualTo("vpnType");
    }

    @Test
    void collectNextField_allRequiredFieldsCollected_returnsComplete() {
        // Given: all required fields collected
        Map<String, String> collectedFields = new HashMap<>();
        collectedFields.put("vpnType", "Add User to VPN Client");
        collectedFields.put("justification", "Need remote access");

        // When: collect next field
        FieldCollectionResult result = fieldCollector.collectNextField("10513", collectedFields);

        // Then: may return optional field or complete
        assertThat(result.isComplete() || result.isOptional())
                .as("Expected either complete or optional field")
                .isTrue();
        if (result.isComplete()) {
            assertThat(result.getCollectedFields()).isEqualTo(collectedFields);
        } else if (result.isOptional()) {
            assertThat(result.getField().isRequired()).isFalse();
        }
    }

    @Test
    void collectNextField_optionalFieldSkipped_continuesFlow() {
        // Given: all required fields and one optional field collected
        Map<String, String> collectedFields = new HashMap<>();
        collectedFields.put("vpnType", "Add User to VPN Client");
        collectedFields.put("deviceType", "Laptop");
        collectedFields.put("justification", "Need remote access");

        // When: collect next field
        FieldCollectionResult result = fieldCollector.collectNextField("10513", collectedFields);

        // Then: collection is complete
        assertThat(result.isComplete()).isTrue();
    }

    @Test
    void validateField_requiredFieldEmpty_returnsInvalid() {
        // When: validate empty required field
        ValidationResult result = fieldCollector.validateField("10504", "applicationName", "");

        // Then: validation fails
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("required");
    }

    @Test
    void validateField_requiredFieldNull_returnsInvalid() {
        // When: validate null required field
        ValidationResult result = fieldCollector.validateField("10504", "applicationName", null);

        // Then: validation fails
        assertThat(result.isValid()).isFalse();
    }

    @Test
    void validateField_validValue_returnsValid() {
        // When: validate with valid value
        ValidationResult result = fieldCollector.validateField("10504", "applicationName", "SAP");

        // Then: validation succeeds
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void validateField_selectFieldWithNumber_returnsNormalizedValue() {
        // When: validate select field with number choice
        ValidationResult result = fieldCollector.validateField("10513", "vpnType", "1");

        // Then: returns normalized value
        assertThat(result.isValid()).isTrue();
        assertThat(result.getNormalizedValue()).isNotBlank();
    }

    @Test
    void validateField_selectFieldWithText_matchesOption() {
        // When: validate select field with text
        ValidationResult result = fieldCollector.validateField("10513", "vpnType", "Add User");

        // Then: matches and normalizes
        assertThat(result.isValid()).isTrue();
        assertThat(result.getNormalizedValue()).contains("Add User");
    }

    @Test
    void validateField_unknownField_acceptsAnyValue() {
        // When: validate unknown field
        ValidationResult result = fieldCollector.validateField("10504", "unknownField", "any value");

        // Then: accepts value
        assertThat(result.isValid()).isTrue();
    }

    @Test
    void getFieldPrompt_englishLanguage_returnsEnglishPrompt() {
        // When: get prompt in English
        String prompt = fieldCollector.getFieldPrompt("10513", "vpnType", "en");

        // Then: returns English prompt
        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("What type of VPN access");
    }

    @Test
    void getFieldPrompt_arabicLanguage_returnsArabicPrompt() {
        // When: get prompt in Arabic
        String prompt = fieldCollector.getFieldPrompt("10513", "vpnType", "ar");

        // Then: returns Arabic prompt
        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("ما نوع الوصول");
    }

    @Test
    void getFieldPrompt_unknownField_returnsFallback() {
        // When: get prompt for unknown field
        String prompt = fieldCollector.getFieldPrompt("10504", "unknownField", "en");

        // Then: returns fallback prompt
        assertThat(prompt).contains("Please provide: unknownField");
    }

    @Test
    void getTotalFieldCount_knownService_returnsCorrectCount() {
        // When: get total field count for VPN service
        int count = fieldCollector.getTotalFieldCount("10513");

        // Then: returns correct count
        assertThat(count).isEqualTo(3);
    }

    @Test
    void getTotalFieldCount_unknownService_returnsDefaultCount() {
        // When: get total field count for unknown service
        int count = fieldCollector.getTotalFieldCount("99999");

        // Then: returns default field count
        assertThat(count).isEqualTo(2);
    }

    @Test
    void getRequiredFieldCount_knownService_returnsCorrectCount() {
        // When: get required field count for VPN service
        int count = fieldCollector.getRequiredFieldCount("10513");

        // Then: returns count of required fields only
        assertThat(count).isGreaterThanOrEqualTo(2);
    }

    @Test
    void getRequiredFieldCount_serverAccess_hasAllRequiredFields() {
        // When: get required field count for server access
        int count = fieldCollector.getRequiredFieldCount("10209");

        // Then: has 4 required fields
        assertThat(count).isEqualTo(4);
    }

    @Test
    void fieldCollectionResult_needsField_hasCorrectProperties() {
        // Given: field definition
        ServiceFieldDefinition field = ServiceFieldDefinition.text("test", "Test", "اختبار", true);

        // When: create needs field result
        FieldCollectionResult result = FieldCollectionResult.needsField(field);

        // Then: has correct properties
        assertThat(result.isComplete()).isFalse();
        assertThat(result.isNeedsInput()).isTrue();
        assertThat(result.isOptional()).isFalse();
        assertThat(result.getField()).isEqualTo(field);
    }

    @Test
    void fieldCollectionResult_optionalField_hasCorrectProperties() {
        // Given: field definition
        ServiceFieldDefinition field = ServiceFieldDefinition.text("test", "Test", "اختبار", false);

        // When: create optional field result
        FieldCollectionResult result = FieldCollectionResult.optionalField(field);

        // Then: has correct properties
        assertThat(result.isComplete()).isFalse();
        assertThat(result.isNeedsInput()).isTrue();
        assertThat(result.isOptional()).isTrue();
        assertThat(result.getField()).isEqualTo(field);
    }

    @Test
    void fieldCollectionResult_complete_hasCorrectProperties() {
        // Given: collected fields
        Map<String, String> fields = new HashMap<>();
        fields.put("field1", "value1");
        fields.put("field2", "value2");

        // When: create complete result
        FieldCollectionResult result = FieldCollectionResult.complete(fields);

        // Then: has correct properties
        assertThat(result.isComplete()).isTrue();
        assertThat(result.isNeedsInput()).isFalse();
        assertThat(result.getCollectedFields()).isEqualTo(fields);
    }

    @Test
    void getFieldsForService_emailManagement_hasCorrectFields() {
        // When: get fields for email management
        List<ServiceFieldDefinition> fields = fieldCollector.getFieldsForService("10242");

        // Then: has request type and details
        assertThat(fields).anyMatch(f -> "requestType".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "details".equals(f.getFieldName()));
    }

    @Test
    void getFieldsForService_technicalIncident_hasSummaryAndDescription() {
        // When: get fields for technical incident
        List<ServiceFieldDefinition> fields = fieldCollector.getFieldsForService("10101");

        // Then: has summary, description, and urgency
        assertThat(fields).anyMatch(f -> "summary".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "description".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "urgency".equals(f.getFieldName()));
    }

    @Test
    void getFieldsForService_carRequest_hasUsageTypeAndPurpose() {
        // When: get fields for car request
        List<ServiceFieldDefinition> fields = fieldCollector.getFieldsForService("10113");

        // Then: has usage type, purpose, and duration
        assertThat(fields).anyMatch(f -> "usageType".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "purpose".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "duration".equals(f.getFieldName()));
    }

    @Test
    void getFieldsForService_shipping_hasShipmentTypeAndDestination() {
        // When: get fields for shipping
        List<ServiceFieldDefinition> fields = fieldCollector.getFieldsForService("10116");

        // Then: has shipment type, destination, and contents
        assertThat(fields).anyMatch(f -> "shipmentType".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "destination".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "contents".equals(f.getFieldName()));
    }

    @Test
    void getFieldsForService_dashboard_hasDataSourceAndKpis() {
        // When: get fields for dashboard service
        List<ServiceFieldDefinition> fields = fieldCollector.getFieldsForService("10601");

        // Then: has dashboard name, data source, and KPIs
        assertThat(fields).anyMatch(f -> "dashboardName".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "dataSource".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "kpis".equals(f.getFieldName()));
    }

    @Test
    void collectNextField_complexService_collectsInOrder() {
        // Given: server access service with multiple fields
        Map<String, String> collectedFields = new HashMap<>();

        // When: collect first field
        FieldCollectionResult result1 = fieldCollector.collectNextField("10209", collectedFields);
        assertThat(result1.isNeedsInput()).isTrue();
        String field1 = result1.getField().getFieldName();
        collectedFields.put(field1, "value1");

        // And: collect second field
        FieldCollectionResult result2 = fieldCollector.collectNextField("10209", collectedFields);
        assertThat(result2.isNeedsInput()).isTrue();
        String field2 = result2.getField().getFieldName();
        collectedFields.put(field2, "value2");

        // And: collect third field
        FieldCollectionResult result3 = fieldCollector.collectNextField("10209", collectedFields);
        assertThat(result3.isNeedsInput()).isTrue();
        String field3 = result3.getField().getFieldName();
        collectedFields.put(field3, "value3");

        // And: collect fourth field
        FieldCollectionResult result4 = fieldCollector.collectNextField("10209", collectedFields);
        assertThat(result4.isNeedsInput()).isTrue();
        String field4 = result4.getField().getFieldName();
        collectedFields.put(field4, "value4");

        // Then: all fields are different
        assertThat(field1).isNotEqualTo(field2);
        assertThat(field2).isNotEqualTo(field3);
        assertThat(field3).isNotEqualTo(field4);

        // And: after all required fields, should be complete
        FieldCollectionResult result5 = fieldCollector.collectNextField("10209", collectedFields);
        assertThat(result5.isComplete()).isTrue();
    }

    @Test
    void validateField_selectFieldInvalidOption_returnsInvalid() {
        // When: validate select field with invalid option
        ValidationResult result = fieldCollector.validateField("10513", "vpnType", "invalid option");

        // Then: validation fails
        assertThat(result.isValid()).isFalse();
        assertThat(result.getErrorMessage()).contains("valid option");
    }

    @Test
    void getFieldsForService_databaseAccess_hasCorrectFields() {
        // When: get fields for database access
        List<ServiceFieldDefinition> fields = fieldCollector.getFieldsForService("10503");

        // Then: has database name, access level, and justification
        assertThat(fields).anyMatch(f -> "databaseName".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "accessLevel".equals(f.getFieldName()));
        assertThat(fields).anyMatch(f -> "justification".equals(f.getFieldName()));
    }
}
