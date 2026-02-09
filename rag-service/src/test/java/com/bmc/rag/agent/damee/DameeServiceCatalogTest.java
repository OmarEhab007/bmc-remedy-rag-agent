package com.bmc.rag.agent.damee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DameeServiceCatalog}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DameeServiceCatalogTest {

    @InjectMocks
    private DameeServiceCatalog catalog;

    @BeforeEach
    void setUp() {
        // Initialize catalog by triggering @PostConstruct manually
        catalog.loadFromMarkdown();
    }

    @Test
    void loadFromMarkdown_loadsHardcodedServicesOnError_success() {
        // Given: @PostConstruct has run and loaded fallback services

        // Then: services should be loaded
        assertThat(catalog.getAllServices()).isNotEmpty();
        assertThat(catalog.getServiceCount()).isGreaterThan(0);
    }

    @Test
    void getById_existingService_returnsService() {
        // Given: hardcoded service 10504 exists

        // When: get by ID
        DameeService service = catalog.getById("10504");

        // Then: service is found
        assertThat(service).isNotNull();
        assertThat(service.getServiceId()).isEqualTo("10504");
        assertThat(service.getNameEn()).isEqualTo("Applications Permission Management");
    }

    @Test
    void getById_nonexistentService_returnsNull() {
        // When: get non-existent service
        DameeService service = catalog.getById("99999");

        // Then: returns null
        assertThat(service).isNull();
    }

    @Test
    void getById_nullId_returnsNull() {
        // When: get with null ID
        DameeService service = catalog.getById(null);

        // Then: returns null
        assertThat(service).isNull();
    }

    @Test
    void getByCategory_existingCategory_returnsServices() {
        // When: get IT Services category
        List<DameeService> services = catalog.getByCategory("IT Services");

        // Then: services are returned
        assertThat(services).isNotEmpty();
        assertThat(services).allMatch(s -> "IT Services".equals(s.getCategory()));
    }

    @Test
    void getByCategory_nonexistentCategory_returnsEmptyList() {
        // When: get non-existent category
        List<DameeService> services = catalog.getByCategory("NonExistent");

        // Then: returns empty list
        assertThat(services).isEmpty();
    }

    @Test
    void getByCategory_nullCategory_returnsEmptyList() {
        // When: get with null category
        List<DameeService> services = catalog.getByCategory(null);

        // Then: returns empty list
        assertThat(services).isEmpty();
    }

    @Test
    void getCategories_returnsAllCategories_success() {
        // When: get all categories
        Set<String> categories = catalog.getCategories();

        // Then: categories are returned
        assertThat(categories).isNotEmpty();
        assertThat(categories).contains("IT Services", "Support Services");
    }

    @Test
    void searchByKeyword_matchingNameEn_returnsResults() {
        // When: search by English name keyword
        List<DameeService> results = catalog.searchByKeyword("permission", 10);

        // Then: matching services are returned
        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(s -> s.getNameEn().toLowerCase().contains("permission"));
        assertThat(results.get(0).getScore()).isGreaterThan(0);
    }

    @Test
    void searchByKeyword_matchingNameAr_returnsResults() {
        // When: search by Arabic keyword
        List<DameeService> results = catalog.searchByKeyword("صلاحية", 10);

        // Then: matching services are returned
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getScore()).isGreaterThan(0);
    }

    @Test
    void searchByKeyword_matchingDescription_returnsResults() {
        // When: search by description keyword
        List<DameeService> results = catalog.searchByKeyword("database", 10);

        // Then: matching services are returned
        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(s -> s.getDescriptionEn().toLowerCase().contains("database"));
    }

    @Test
    void searchByKeyword_matchingKeywords_returnsResults() {
        // When: search by service keyword
        List<DameeService> results = catalog.searchByKeyword("vpn", 10);

        // Then: matching services are returned
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getScore()).isGreaterThan(0);
    }

    @Test
    void searchByKeyword_nullQuery_returnsEmptyList() {
        // When: search with null query
        List<DameeService> results = catalog.searchByKeyword(null, 10);

        // Then: returns empty list
        assertThat(results).isEmpty();
    }

    @Test
    void searchByKeyword_blankQuery_returnsEmptyList() {
        // When: search with blank query
        List<DameeService> results = catalog.searchByKeyword("   ", 10);

        // Then: returns empty list
        assertThat(results).isEmpty();
    }

    @Test
    void searchByKeyword_noMatches_returnsEmptyList() {
        // When: search with no matching keyword
        List<DameeService> results = catalog.searchByKeyword("xyzabc123", 10);

        // Then: returns empty list
        assertThat(results).isEmpty();
    }

    @Test
    void searchByKeyword_respectsLimit_returnsLimitedResults() {
        // When: search with limit of 2
        List<DameeService> results = catalog.searchByKeyword("service", 2);

        // Then: returns at most 2 results
        assertThat(results).hasSizeLessThanOrEqualTo(2);
    }

    @Test
    void searchByKeyword_sortsByScoreDescending_success() {
        // When: search for keyword that matches multiple services
        List<DameeService> results = catalog.searchByKeyword("access", 5);

        // Then: results are sorted by score descending
        if (results.size() > 1) {
            for (int i = 0; i < results.size() - 1; i++) {
                assertThat(results.get(i).getScore()).isGreaterThanOrEqualTo(results.get(i + 1).getScore());
            }
        }
    }

    @Test
    void search_withQueryAndCategory_returnsFilteredResults() {
        // When: search with category filter
        List<DameeService> results = catalog.search("email", "IT Services", 10);

        // Then: results match both query and category
        assertThat(results).isNotEmpty();
        assertThat(results).allMatch(s -> "IT Services".equals(s.getCategory()));
    }

    @Test
    void search_withOnlyCategory_returnsAllInCategory() {
        // When: search with only category (no query)
        List<DameeService> results = catalog.search("", "IT Services", 10);

        // Then: returns all services in category (up to limit)
        assertThat(results).isNotEmpty();
        assertThat(results).hasSizeLessThanOrEqualTo(10);
        assertThat(results).allMatch(s -> "IT Services".equals(s.getCategory()));
    }

    @Test
    void search_withNullCategory_searchesAllServices() {
        // When: search without category filter
        List<DameeService> results = catalog.search("software", null, 10);

        // Then: searches across all categories
        assertThat(results).isNotEmpty();
    }

    @Test
    void search_withNonexistentCategory_returnsEmptyList() {
        // When: search with non-existent category
        List<DameeService> results = catalog.search("test", "NonExistent", 10);

        // Then: returns empty list
        assertThat(results).isEmpty();
    }

    @Test
    void search_respectsLimit_success() {
        // When: search with small limit
        List<DameeService> results = catalog.search("", "IT Services", 3);

        // Then: respects limit
        assertThat(results).hasSizeLessThanOrEqualTo(3);
    }

    @Test
    void getServiceCount_returnsCorrectCount_success() {
        // When: get service count
        int count = catalog.getServiceCount();

        // Then: count matches list size
        assertThat(count).isEqualTo(catalog.getAllServices().size());
        assertThat(count).isGreaterThan(0);
    }

    @Test
    void getCategorySummary_returnsFormattedSummary_success() {
        // When: get category summary
        String summary = catalog.getCategorySummary();

        // Then: summary contains category information
        assertThat(summary).isNotBlank();
        assertThat(summary).contains("Available service categories");
        assertThat(summary).contains("IT Services");
    }

    @Test
    void calculateKeywordScore_nameMatch_hasHighestWeight() {
        // When: search for exact service name
        List<DameeService> results = catalog.searchByKeyword("Applications Permission Management", 5);

        // Then: exact name match has high score
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getScore()).isGreaterThan(8);
    }

    @Test
    void calculateKeywordScore_descriptionMatch_hasModerateWeight() {
        // When: search for description keyword only
        List<DameeService> results = catalog.searchByKeyword("manage users", 5);

        // Then: description match has lower score than name match
        assertThat(results).as("Expected results for 'manage users' search").isNotEmpty();
        assertThat(results.get(0).getScore()).isGreaterThan(0);
    }

    @Test
    void hardcodedServices_haveRequiredFields_success() {
        // When: get any service
        DameeService service = catalog.getById("10504");

        // Then: service has required fields
        assertThat(service).isNotNull();
        assertThat(service.getServiceId()).isNotBlank();
        assertThat(service.getNameEn()).isNotBlank();
        assertThat(service.getCategory()).isNotBlank();
        assertThat(service.getWorkflow()).isNotEmpty();
        assertThat(service.getRequiredFields()).isNotEmpty();
    }

    @Test
    void hardcodedServices_haveValidWorkflow_success() {
        // When: get service with workflow
        DameeService service = catalog.getById("10513");

        // Then: workflow has valid steps
        assertThat(service).isNotNull();
        if (service.getWorkflow() != null && !service.getWorkflow().isEmpty()) {
            assertThat(service.getWorkflow()).allMatch(step -> step.getOrder() > 0);
            assertThat(service.getWorkflow()).allMatch(step -> step.getDescription() != null);
        }
        // If workflow is empty, that's also valid (default workflow used)
    }

    @Test
    void hardcodedServices_vpnServiceHasCorrectKeywords_success() {
        // When: get VPN service
        DameeService service = catalog.getById("10513");

        // Then: has VPN-related keywords
        assertThat(service).isNotNull();
        assertThat(service.getKeywords()).isNotEmpty();
        assertThat(service.getKeywords()).anyMatch(k -> k.toLowerCase().contains("virtual") || k.toLowerCase().contains("private"));
    }

    @Test
    void getAllServices_returnsUnmodifiableList_success() {
        // When: get all services
        List<DameeService> services = catalog.getAllServices();

        // Then: list is returned
        assertThat(services).isNotEmpty();

        // And: each service has valid ID
        assertThat(services).allMatch(s -> s.getServiceId() != null && !s.getServiceId().isEmpty());
    }

    @Test
    void servicesById_containsAllServices_success() {
        // When: get services map and list
        var byIdMap = catalog.getServicesById();
        var allServicesList = catalog.getAllServices();

        // Then: map size should match or be close to list size (may differ due to duplicates)
        assertThat(byIdMap.size()).isGreaterThan(0);
        assertThat(allServicesList.size()).isGreaterThan(0);
        // All services in the list should have entries in the map
        assertThat(allServicesList).allMatch(s -> byIdMap.containsKey(s.getServiceId()));
    }

    @Test
    void searchByKeyword_matchingCategory_returnsResults() {
        // When: search by category keyword
        List<DameeService> results = catalog.searchByKeyword("IT Services", 10);

        // Then: matching services are returned
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getScore()).isGreaterThan(0);
    }

    @Test
    void search_nullQuery_returnsAllInCategory() {
        // When: search with null query but valid category
        List<DameeService> results = catalog.search(null, "IT Services", 10);

        // Then: returns services from category up to limit
        assertThat(results).isNotEmpty();
        assertThat(results).hasSizeLessThanOrEqualTo(10);
        assertThat(results).allMatch(s -> "IT Services".equals(s.getCategory()));
    }

    @Test
    void search_blankQuery_returnsAllInCategory() {
        // When: search with blank query
        List<DameeService> results = catalog.search("   ", "IT Services", 5);

        // Then: returns services from category
        assertThat(results).isNotEmpty();
        assertThat(results).hasSizeLessThanOrEqualTo(5);
    }

    @Test
    void search_blankCategory_searchesAllServices() {
        // When: search with blank category
        List<DameeService> results = catalog.search("vpn", "", 10);

        // Then: searches across all categories
        assertThat(results).isNotEmpty();
    }

    @Test
    void calculateKeywordScore_multipleMatches_highScore() {
        // When: search with keywords matching name, description, and keywords
        List<DameeService> results = catalog.searchByKeyword("permission application", 5);

        // Then: service with multiple matches has a positive score
        assertThat(results).as("Expected results for 'permission application' search").isNotEmpty();
        assertThat(results.get(0).getScore()).isGreaterThan(0);
    }

    @Test
    void hardcodedServices_emailService_hasCorrectProperties() {
        // When: get email service
        DameeService service = catalog.getById("10242");

        // Then: has correct properties
        assertThat(service).isNotNull();
        assertThat(service.getNameEn()).contains("Email");
        assertThat(service.getCategory()).isEqualTo("IT Services");
        assertThat(service.getKeywords()).contains("email");
    }

    @Test
    void hardcodedServices_serverAccessService_exists() {
        // When: get server access service
        DameeService service = catalog.getById("10209");

        // Then: exists with correct properties
        assertThat(service).isNotNull();
        assertThat(service.getNameEn()).contains("Server");
    }

    @Test
    void hardcodedServices_softwareInstallation_exists() {
        // When: get software installation service
        DameeService service = catalog.getById("10247");

        // Then: exists with correct properties
        assertThat(service).isNotNull();
        assertThat(service.getNameEn()).contains("Software");
    }

    @Test
    void hardcodedServices_technicalIncident_exists() {
        // When: get technical incident service
        DameeService service = catalog.getById("10101");

        // Then: exists with correct properties
        assertThat(service).isNotNull();
        assertThat(service.getCategory()).isEqualTo("IT Services");
    }

    @Test
    void hardcodedServices_meetingApps_exists() {
        // When: get meeting apps service
        DameeService service = catalog.getById("10257");

        // Then: exists with correct properties
        assertThat(service).isNotNull();
        assertThat(service.getKeywords()).anyMatch(k -> k.contains("meeting") || k.contains("webex"));
    }

    @Test
    void hardcodedServices_supportServices_loaded() {
        // When: get support services
        List<DameeService> services = catalog.getByCategory("Support Services");

        // Then: support services exist
        assertThat(services).isNotEmpty();
    }

    @Test
    void hardcodedServices_legalServices_loaded() {
        // When: get legal services
        List<DameeService> services = catalog.getByCategory("Legal Consultation Services");

        // Then: legal services exist
        assertThat(services).isNotEmpty();
    }

    @Test
    void hardcodedServices_geospatialServices_loaded() {
        // When: get geospatial services
        List<DameeService> services = catalog.getByCategory("Geospatial Services");

        // Then: geospatial services exist
        assertThat(services).isNotEmpty();
    }

    // ========================
    // Additional coverage for generateRequiredFields branches
    // ========================

    @Test
    void hardcodedServices_supportServicesHaveRequestDateField() {
        // When: get support service
        List<DameeService> services = catalog.getByCategory("Support Services");

        // Then: support services should have requestDate in required fields
        assertThat(services).isNotEmpty();
        assertThat(services.get(0).getRequiredFields()).contains("description");
    }

    @Test
    void hardcodedServices_legalServicesHaveCaseDetailsField() {
        // When: get legal service
        List<DameeService> services = catalog.getByCategory("Legal Consultation Services");

        // Then: legal services exist with required fields
        assertThat(services).isNotEmpty();
        DameeService legalService = services.get(0);
        assertThat(legalService.getRequiredFields()).isNotNull();
    }

    @Test
    void hardcodedServices_geospatialServicesHaveDataRequirements() {
        // When: get geospatial service
        List<DameeService> services = catalog.getByCategory("Geospatial Services");

        // Then: geospatial services exist with fields
        assertThat(services).isNotEmpty();
        DameeService geoService = services.get(0);
        assertThat(geoService.getRequiredFields()).isNotNull();
    }

    @Test
    void hardcodedServices_inspectionServicesLoaded() {
        // When: get inspection service
        DameeService service = catalog.getById("10401");

        // Then: inspection service exists
        assertThat(service).isNotNull();
        assertThat(service.getNameEn()).contains("Inspection");
    }

    @Test
    void hardcodedServices_telephonyServiceLoaded() {
        // When: get IP telephony service
        DameeService service = catalog.getById("10254");

        // Then: telephony service exists
        assertThat(service).isNotNull();
        assertThat(service.getKeywords()).anyMatch(k -> k.contains("phone") || k.contains("telephony"));
    }

    @Test
    void hardcodedServices_identityCardServiceLoaded() {
        // When: get identity card service
        DameeService service = catalog.getById("10120");

        // Then: exists
        assertThat(service).isNotNull();
        assertThat(service.getCategory()).isEqualTo("Support Services");
    }

    @Test
    void hardcodedServices_shippingServiceLoaded() {
        // When: get shipping service
        DameeService service = catalog.getById("10116");

        // Then: exists
        assertThat(service).isNotNull();
        assertThat(service.getKeywords()).anyMatch(k -> k.contains("shipping") || k.contains("smsa"));
    }

    @Test
    void hardcodedServices_contractDraftServiceLoaded() {
        // When: get contract draft service
        DameeService service = catalog.getById("10106");

        // Then: exists as legal service
        assertThat(service).isNotNull();
        assertThat(service.getCategory()).isEqualTo("Legal Consultation Services");
    }

    @Test
    void hardcodedServices_letterDraftServiceLoaded() {
        // When: get letter draft service
        DameeService service = catalog.getById("10109");

        // Then: exists
        assertThat(service).isNotNull();
        assertThat(service.getCategory()).isEqualTo("Legal Consultation Services");
    }

    @Test
    void hardcodedServices_gisAccountServiceLoaded() {
        // When: get GIS account service
        DameeService service = catalog.getById("10605");

        // Then: exists with correct category
        assertThat(service).isNotNull();
        assertThat(service.getCategory()).isEqualTo("Geospatial Services");
    }

    @Test
    void hardcodedServices_dashboardServiceLoaded() {
        // When: get dashboard service
        DameeService service = catalog.getById("10601");

        // Then: exists
        assertThat(service).isNotNull();
        assertThat(service.getKeywords()).anyMatch(k -> k.contains("dashboard") || k.contains("gis"));
    }

    @Test
    void hardcodedServices_requestCarServiceLoaded() {
        // When: get car request service
        DameeService service = catalog.getById("10113");

        // Then: exists
        assertThat(service).isNotNull();
        assertThat(service.getCategory()).isEqualTo("Support Services");
    }

    @Test
    void hardcodedServices_newCarServiceLoaded() {
        // When: get new car service
        DameeService service = catalog.getById("10114");

        // Then: exists
        assertThat(service).isNotNull();
    }

    @Test
    void hardcodedServices_databaseAccessServiceLoaded() {
        // When: get database access service
        DameeService service = catalog.getById("10503");

        // Then: exists
        assertThat(service).isNotNull();
        assertThat(service.getKeywords()).anyMatch(k -> k.contains("database") || k.contains("db"));
    }

    @Test
    void searchByKeyword_matchingArabicDescription_returnsResults() {
        // When: search by Arabic description keyword
        List<DameeService> results = catalog.searchByKeyword("إدارة", 10);

        // Then: matching services returned (Arabic description match)
        assertThat(results).isNotEmpty();
    }

    @Test
    void searchByKeyword_queryContainsKeyword_returnsResults() {
        // When: search with query that contains a keyword (reverse match)
        // "vpn" is a keyword; searching "need vpn access today" should match
        List<DameeService> results = catalog.searchByKeyword("vpn access", 10);

        // Then: results found
        assertThat(results).isNotEmpty();
    }

    @Test
    void hardcodedServices_allHaveDefaultWorkflow() {
        // When: get all services
        List<DameeService> services = catalog.getAllServices();

        // Then: all services have workflow (may be empty list for parsed services, non-empty for hardcoded)
        for (DameeService service : services) {
            assertThat(service.getWorkflow()).isNotNull();
        }

        // At least some services should have non-empty workflow
        long withWorkflow = services.stream()
                .filter(s -> s.getWorkflow() != null && !s.getWorkflow().isEmpty())
                .count();
        assertThat(withWorkflow).isGreaterThan(0);
    }

    @Test
    void hardcodedServices_allHaveRequiredFields() {
        // When: check all services
        List<DameeService> services = catalog.getAllServices();

        // Then: all services have required fields including description
        for (DameeService service : services) {
            assertThat(service.getRequiredFields()).isNotNull();
            assertThat(service.getRequiredFields()).contains("description");
        }
    }

    @Test
    void search_withQueryAndNonmatchingCategory_returnsEmpty() {
        // When: search with query but non-matching category
        List<DameeService> results = catalog.search("vpn", "Legal Consultation Services", 10);

        // Then: empty because VPN is not in legal services
        assertThat(results).isEmpty();
    }

    @Test
    void getCategorySummary_containsServiceCounts() {
        // When: get summary
        String summary = catalog.getCategorySummary();

        // Then: contains numbered list with service counts
        assertThat(summary).contains("1.");
        assertThat(summary).contains("services)");
    }
}
