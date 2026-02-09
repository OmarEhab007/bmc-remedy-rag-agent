package com.bmc.rag.agent.tools;

import com.bmc.rag.agent.damee.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DameeServiceToolTest {

    @Mock
    private DameeServiceCatalog catalog;

    @Mock
    private ServiceIntentMatcher intentMatcher;

    @Mock
    private GuidedServiceCreator guidedCreator;

    @Mock
    private WorkflowPreviewBuilder workflowBuilder;

    @InjectMocks
    private DameeServiceTool tool;

    private DameeService sampleService;

    @BeforeEach
    void setUp() {
        sampleService = DameeService.builder()
                .serviceId("10504")
                .nameEn("VPN Access Request")
                .nameAr("طلب وصول VPN")
                .descriptionEn("Request VPN access for remote work")
                .descriptionAr("طلب وصول VPN للعمل عن بعد")
                .category("IT Services")
                .subcategory("Network")
                .url("https://damee.example.com/service/10504")
                .requiresManagerApproval(true)
                .vipBypass(true)
                .keywords(List.of("vpn", "remote", "network"))
                .workflow(List.of(
                        DameeService.WorkflowStep.builder()
                                .description("Submit Request").requiresApproval(false).build(),
                        DameeService.WorkflowStep.builder()
                                .description("Manager Approval").requiresApproval(true).build()
                ))
                .build();
    }

    @Nested
    @DisplayName("searchServices")
    class SearchServices {

        @Test
        void searchServices_withCategory_usesFilteredSearch() {
            when(catalog.search("vpn", "IT Services", 5))
                    .thenReturn(List.of(sampleService));

            String result = tool.searchServices("vpn", "IT Services");
            assertThat(result).contains("VPN Access Request");
            assertThat(result).contains("10504");
            assertThat(result).contains("IT Services");
            verify(catalog).search("vpn", "IT Services", 5);
        }

        @Test
        void searchServices_noCategory_usesKeywordSearch() {
            when(catalog.searchByKeyword("vpn", 5))
                    .thenReturn(List.of(sampleService));

            String result = tool.searchServices("vpn", null);
            assertThat(result).contains("VPN Access Request");
            verify(catalog).searchByKeyword("vpn", 5);
        }

        @Test
        void searchServices_blankCategory_usesKeywordSearch() {
            when(catalog.searchByKeyword("vpn", 5))
                    .thenReturn(List.of(sampleService));

            String result = tool.searchServices("vpn", "  ");
            assertThat(result).contains("VPN Access Request");
            verify(catalog).searchByKeyword("vpn", 5);
        }

        @Test
        void searchServices_noResults_returnsCategorySummary() {
            when(catalog.searchByKeyword("unknown", 5)).thenReturn(List.of());
            when(catalog.getCategorySummary()).thenReturn("IT Services (10)\nSupport Services (5)");

            String result = tool.searchServices("unknown", null);
            assertThat(result).contains("No matching services");
            assertThat(result).contains("IT Services (10)");
        }

        @Test
        void searchServices_withArabicName_includesArabic() {
            when(catalog.searchByKeyword("vpn", 5)).thenReturn(List.of(sampleService));

            String result = tool.searchServices("vpn", null);
            assertThat(result).contains("طلب وصول VPN");
        }

        @Test
        void searchServices_longDescription_truncated() {
            DameeService longDescService = DameeService.builder()
                    .serviceId("S1").nameEn("Test")
                    .descriptionEn("A".repeat(200))
                    .category("IT").build();
            when(catalog.searchByKeyword("test", 5)).thenReturn(List.of(longDescService));

            String result = tool.searchServices("test", null);
            assertThat(result).contains("...");
        }

        @Test
        void searchServices_withUrl_includesLink() {
            when(catalog.searchByKeyword("vpn", 5)).thenReturn(List.of(sampleService));

            String result = tool.searchServices("vpn", null);
            assertThat(result).contains("Open in Damee");
            assertThat(result).contains("https://damee.example.com/service/10504");
        }
    }

    @Nested
    @DisplayName("getServiceDetails")
    class GetServiceDetails {

        @Test
        void getServiceDetails_found_returnsFullDetails() {
            when(catalog.getById("10504")).thenReturn(sampleService);
            when(workflowBuilder.buildCompactWorkflow(sampleService))
                    .thenReturn("Submit → Manager Approval");

            String result = tool.getServiceDetails("10504");
            assertThat(result).contains("VPN Access Request");
            assertThat(result).contains("طلب وصول VPN");
            assertThat(result).contains("10504");
            assertThat(result).contains("IT Services");
            assertThat(result).contains("Network");
            assertThat(result).contains("Request VPN access");
            assertThat(result).contains("Manager Approval Required: Yes");
            assertThat(result).contains("VIP Bypass Available: Yes");
            assertThat(result).contains("vpn, remote, network");
            assertThat(result).contains("Submit");
        }

        @Test
        void getServiceDetails_notFound_returnsError() {
            when(catalog.getById("99999")).thenReturn(null);

            String result = tool.getServiceDetails("99999");
            assertThat(result).contains("Service not found");
            assertThat(result).contains("99999");
        }
    }

    @Nested
    @DisplayName("listCategories")
    class ListCategories {

        @Test
        void listCategories_returnsCategorySummary() {
            when(catalog.getCategorySummary()).thenReturn("IT Services (10)\nSupport (5)");
            when(catalog.getServiceCount()).thenReturn(15);

            String result = tool.listCategories();
            assertThat(result).contains("Damee Service Categories");
            assertThat(result).contains("IT Services (10)");
            assertThat(result).contains("Total services available: 15");
        }
    }

    @Nested
    @DisplayName("getServicesByCategory")
    class GetServicesByCategory {

        @Test
        void getServicesByCategory_found_listsServices() {
            when(catalog.getByCategory("IT Services")).thenReturn(List.of(sampleService));

            String result = tool.getServicesByCategory("IT Services");
            assertThat(result).contains("IT Services (1 service)");
            assertThat(result).contains("VPN Access Request");
            assertThat(result).contains("10504");
        }

        @Test
        void getServicesByCategory_notFound_returnsCategorySummary() {
            when(catalog.getByCategory("Unknown")).thenReturn(List.of());
            when(catalog.getCategorySummary()).thenReturn("Available categories list");

            String result = tool.getServicesByCategory("Unknown");
            assertThat(result).contains("No services found");
            assertThat(result).contains("Available categories list");
        }
    }

    @Nested
    @DisplayName("matchUserIntent")
    class MatchUserIntent {

        @Test
        void matchUserIntent_delegatesToMatcher() {
            var matchResult = mock(ServiceIntentMatcher.ServiceMatchResult.class);
            when(matchResult.getDisplayMessage()).thenReturn("Matched: VPN Access Request");
            when(intentMatcher.matchService("I need VPN")).thenReturn(matchResult);

            String result = tool.matchUserIntent("I need VPN");
            assertThat(result).isEqualTo("Matched: VPN Access Request");
            verify(intentMatcher).matchService("I need VPN");
        }
    }

    @Nested
    @DisplayName("startGuidedRequest")
    class StartGuidedRequest {

        @Test
        void startGuidedRequest_serviceNotFound_returnsError() {
            when(catalog.getById("99999")).thenReturn(null);

            String result = tool.startGuidedRequest("99999", "session-1", "user-1");
            assertThat(result).contains("Service not found");
        }

        @Test
        void startGuidedRequest_serviceFound_startsGuidedFlow() {
            when(catalog.getById("10504")).thenReturn(sampleService);
            var guidedResponse = mock(GuidedServiceCreator.GuidedResponse.class);
            when(guidedResponse.isError()).thenReturn(false);
            when(guidedResponse.getMessage()).thenReturn("Please provide your reason for VPN access:");
            when(guidedCreator.processMessage("session-1", "user-1", "start 10504"))
                    .thenReturn(guidedResponse);

            String result = tool.startGuidedRequest("10504", "session-1", "user-1");
            assertThat(result).contains("VPN Access Request");
            assertThat(result).contains("Please provide your reason");
        }

        @Test
        void startGuidedRequest_guidedError_returnsError() {
            when(catalog.getById("10504")).thenReturn(sampleService);
            var guidedResponse = mock(GuidedServiceCreator.GuidedResponse.class);
            when(guidedResponse.isError()).thenReturn(true);
            when(guidedResponse.getErrorMessage()).thenReturn("Session limit reached");
            when(guidedCreator.processMessage(anyString(), anyString(), anyString()))
                    .thenReturn(guidedResponse);

            String result = tool.startGuidedRequest("10504", "session-1", "user-1");
            assertThat(result).contains("Error");
            assertThat(result).contains("Session limit reached");
        }
    }

    @Nested
    @DisplayName("getServiceWorkflow")
    class GetServiceWorkflow {

        @Test
        void getServiceWorkflow_found_returnsWorkflow() {
            when(catalog.getById("10504")).thenReturn(sampleService);
            when(workflowBuilder.buildDetailedWorkflow(sampleService, null))
                    .thenReturn("Step 1: Submit\nStep 2: Approve");

            String result = tool.getServiceWorkflow("10504");
            assertThat(result).contains("Workflow for: VPN Access Request");
            assertThat(result).contains("Step 1: Submit");
            assertThat(result).contains("VIP users may bypass");
            assertThat(result).contains("Manager approval is required");
        }

        @Test
        void getServiceWorkflow_notFound_returnsError() {
            when(catalog.getById("99999")).thenReturn(null);

            String result = tool.getServiceWorkflow("99999");
            assertThat(result).contains("Service not found");
        }
    }
}
