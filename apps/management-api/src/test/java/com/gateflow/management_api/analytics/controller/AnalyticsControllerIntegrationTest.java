package com.gateflow.management_api.analytics.controller;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.gateflow.management_api.organization.entity.Organization;
import com.gateflow.management_api.organization.entity.OrganizationStatus;
import com.gateflow.management_api.organization.repository.OrganizationRepository;
import com.gateflow.management_api.user.entity.MembershipStatus;
import com.gateflow.management_api.user.entity.OrganizationMembership;
import com.gateflow.management_api.user.entity.OrganizationRole;
import com.gateflow.management_api.user.entity.User;
import com.gateflow.management_api.user.repository.OrganizationMembershipRepository;
import com.gateflow.management_api.user.repository.UserRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AnalyticsControllerIntegrationTest {

    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    private String ownerToken;
    private String memberToken;
    private String outsiderToken;
    private UUID organizationId;
    private UUID secondOrgId;

    @BeforeAll
    void setUp() throws Exception {
        ownerToken = registerAndLogin("analytics-owner@test.com");
        memberToken = registerAndLogin("analytics-member@test.com");
        outsiderToken = registerAndLogin("analytics-outsider@test.com");

        // Create main organization
        MvcResult result = mockMvc.perform(
                post("/api/v1/organizations")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Analytics Test Organization",
                                  "slug": "analytics-test-org",
                                  "description": "Testing Analytics Controller"
                                }
                                """)
        ).andExpect(status().isCreated()).andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        organizationId = UUID.fromString(response.get("id").asText());

        // Add member
        addMembership("analytics-member@test.com", organizationId, OrganizationRole.DEVELOPER);

        // Create second organization where outsider is owner
        MvcResult secondResult = mockMvc.perform(
                post("/api/v1/organizations")
                        .header("Authorization", bearer(outsiderToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Second Organization",
                                  "slug": "second-analytics-org",
                                  "description": "Outsider Org"
                                }
                                """)
        ).andExpect(status().isCreated()).andReturn();

        JsonNode secondResponse = objectMapper.readTree(secondResult.getResponse().getContentAsString());
        secondOrgId = UUID.fromString(secondResponse.get("id").asText());
    }

    @Test
    void getOverview_unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(
                get("/api/v1/organizations/{id}/analytics/overview", organizationId)
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void getOverview_authorizedOwner_shouldReturn200WithCorrectDto() throws Exception {
        mockMvc.perform(
                get("/api/v1/organizations/{id}/analytics/overview", organizationId)
                        .header("Authorization", bearer(ownerToken))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRequests", notNullValue()))
                .andExpect(jsonPath("$.successfulRequests", notNullValue()))
                .andExpect(jsonPath("$.failedRequests", notNullValue()))
                .andExpect(jsonPath("$.successRate", notNullValue()))
                .andExpect(jsonPath("$.avgLatencyMs", notNullValue()))
                .andExpect(jsonPath("$.period", is("7d")));
    }

    @Test
    void getOverview_withExplicitRange_shouldReturn200() throws Exception {
        mockMvc.perform(
                get("/api/v1/organizations/{id}/analytics/overview", organizationId)
                        .param("range", "24h")
                        .header("Authorization", bearer(ownerToken))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period", is("24h")));
    }

    @Test
    void getOverview_withInvalidRange_shouldReturn400() throws Exception {
        mockMvc.perform(
                get("/api/v1/organizations/{id}/analytics/overview", organizationId)
                        .param("range", "invalid-range")
                        .header("Authorization", bearer(ownerToken))
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", notNullValue()));
    }

    @Test
    void getTraffic_authorized_shouldReturn200WithDefaultInterval() throws Exception {
        mockMvc.perform(
                get("/api/v1/organizations/{id}/analytics/traffic", organizationId)
                        .header("Authorization", bearer(ownerToken))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", notNullValue()))
                .andExpect(jsonPath("$.total", notNullValue()))
                .andExpect(jsonPath("$.successful", notNullValue()))
                .andExpect(jsonPath("$.failed", notNullValue()));
    }

    @Test
    void getTraffic_withExplicitInterval_shouldReturn200() throws Exception {
        mockMvc.perform(
                get("/api/v1/organizations/{id}/analytics/traffic", organizationId)
                        .param("range", "30d")
                        .param("interval", "week")
                        .header("Authorization", bearer(ownerToken))
        )
                .andExpect(status().isOk());
    }

    @Test
    void getTraffic_withInvalidInterval_shouldReturn400() throws Exception {
        mockMvc.perform(
                get("/api/v1/organizations/{id}/analytics/traffic", organizationId)
                        .param("interval", "minute")
                        .header("Authorization", bearer(ownerToken))
        )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void getTopApis_authorized_shouldReturn200() throws Exception {
        mockMvc.perform(
                get("/api/v1/organizations/{id}/analytics/top-apis", organizationId)
                        .header("Authorization", bearer(ownerToken))
        )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", notNullValue()));
    }

    @Test
    void getTopApis_withInvalidLimit_shouldReturn400() throws Exception {
        mockMvc.perform(
                get("/api/v1/organizations/{id}/analytics/top-apis", organizationId)
                        .param("limit", "0")
                        .header("Authorization", bearer(ownerToken))
        ).andExpect(status().isBadRequest());

        mockMvc.perform(
                get("/api/v1/organizations/{id}/analytics/top-apis", organizationId)
                        .param("limit", "101")
                        .header("Authorization", bearer(ownerToken))
        ).andExpect(status().isBadRequest());
    }

    @Test
    void crossOrganizationAccess_shouldReturn403() throws Exception {
        // Outsider attempts to access organizationId
        mockMvc.perform(
                get("/api/v1/organizations/{id}/analytics/overview", organizationId)
                        .header("Authorization", bearer(outsiderToken))
        ).andExpect(status().isForbidden());

        // Owner of org 1 attempts to access secondOrgId
        mockMvc.perform(
                get("/api/v1/organizations/{id}/analytics/overview", secondOrgId)
                        .header("Authorization", bearer(ownerToken))
        ).andExpect(status().isForbidden());
    }

    @Test
    void suspendedMembership_shouldReturn403() throws Exception {
        // Suspend membership for memberToken
        User user = userRepository.findByEmailIgnoreCase("analytics-member@test.com").orElseThrow();
        OrganizationMembership membership = membershipRepository
                .findByUserIdAndOrganizationId(user.getId(), organizationId)
                .orElseThrow();

        membership.setStatus(MembershipStatus.SUSPENDED);
        membershipRepository.saveAndFlush(membership);

        try {
            mockMvc.perform(
                    get("/api/v1/organizations/{id}/analytics/overview", organizationId)
                            .header("Authorization", bearer(memberToken))
            ).andExpect(status().isForbidden());
        } finally {
            membership.setStatus(MembershipStatus.ACTIVE);
            membershipRepository.saveAndFlush(membership);
        }
    }

    @Test
    void suspendedOrganization_shouldReturn403() throws Exception {
        Organization org = organizationRepository.findById(organizationId).orElseThrow();
        org.setStatus(OrganizationStatus.SUSPENDED);
        organizationRepository.saveAndFlush(org);

        try {
            mockMvc.perform(
                    get("/api/v1/organizations/{id}/analytics/overview", organizationId)
                            .header("Authorization", bearer(ownerToken))
            ).andExpect(status().isForbidden());
        } finally {
            org.setStatus(OrganizationStatus.ACTIVE);
            organizationRepository.saveAndFlush(org);
        }
    }

    private void addMembership(String email, UUID orgId, OrganizationRole role) {
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        Organization organization = organizationRepository.findById(orgId).orElseThrow();

        OrganizationMembership membership = new OrganizationMembership();
        membership.setUser(user);
        membership.setOrganization(organization);
        membership.setRole(role);

        membershipRepository.saveAndFlush(membership);
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of(
                                        "fullName", "Analytics Test User",
                                        "email", email,
                                        "password", "TestPassword123"
                                )
                        ))
        ).andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of(
                                        "email", email,
                                        "password", "TestPassword123"
                                )
                        ))
        ).andExpect(status().isOk()).andReturn();

        JsonNode loginResponse = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        return loginResponse.get("accessToken").asText();
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
