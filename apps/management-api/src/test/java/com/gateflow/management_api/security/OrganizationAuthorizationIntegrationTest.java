package com.gateflow.management_api.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.testcontainers.postgresql.PostgreSQLContainer;

import com.gateflow.management_api.organization.entity.Organization;
import com.gateflow.management_api.organization.repository.OrganizationRepository;
import com.gateflow.management_api.organization.entity.OrganizationStatus;
import com.gateflow.management_api.user.entity.MembershipStatus;
import com.gateflow.management_api.user.entity.OrganizationMembership;
import com.gateflow.management_api.user.entity.OrganizationRole;
import com.gateflow.management_api.user.entity.User;
import com.gateflow.management_api.user.repository.OrganizationMembershipRepository;
import com.gateflow.management_api.user.repository.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrganizationAuthorizationIntegrationTest {

    static final PostgreSQLContainer postgres
            = new PostgreSQLContainer("postgres:16-alpine");

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
    private String adminToken;
    private String developerToken;
    private String viewerToken;

    private UUID organizationId;

    @BeforeAll
    void setUp() throws Exception {

        ownerToken = registerAndLogin("owner@test.com");
        adminToken = registerAndLogin("admin@test.com");
        developerToken = registerAndLogin("developer@test.com");
        viewerToken = registerAndLogin("viewer@test.com");

        MvcResult result = mockMvc.perform(
                post("/api/v1/organizations")
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Integration Test Organization",
                                  "slug": "integration-test-org",
                                  "description": "Security integration testing"
                                }
                                """)
        ).andExpect(status().isCreated()).andReturn();

        JsonNode response = objectMapper.readTree(
                result.getResponse().getContentAsString()
        );

        organizationId = UUID.fromString(response.get("id").asText());

        addMembership("admin@test.com", OrganizationRole.ADMIN);
        addMembership("developer@test.com", OrganizationRole.DEVELOPER);
        addMembership("viewer@test.com", OrganizationRole.VIEWER);
    }

    @Test
    void shouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(
                get("/api/v1/organizations/{id}", organizationId)
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldAllowOwnerToViewOrganization() throws Exception {
        assertCanView(ownerToken);
    }

    @Test
    void shouldAllowAdminToViewOrganization() throws Exception {
        assertCanView(adminToken);
    }

    @Test
    void shouldAllowDeveloperToViewOrganization() throws Exception {
        assertCanView(developerToken);
    }

    @Test
    void shouldAllowViewerToViewOrganization() throws Exception {
        assertCanView(viewerToken);
    }

    @Test
    void shouldRejectNonMemberFromViewingOrganization() throws Exception {

        String outsiderToken = registerAndLogin("outsider@test.com");

        mockMvc.perform(
                get("/api/v1/organizations/{id}", organizationId)
                        .header("Authorization", bearer(outsiderToken))
        ).andExpect(status().isForbidden());
    }

    @Test
    void shouldRejectMemberWithSuspendedMembership() throws Exception {

        User viewer = userRepository.findByEmailIgnoreCase("viewer@test.com")
                .orElseThrow();

        OrganizationMembership membership = membershipRepository
                .findByUserIdAndOrganizationId(viewer.getId(), organizationId)
                .orElseThrow();

        membership.setStatus(MembershipStatus.SUSPENDED);
        membershipRepository.saveAndFlush(membership);

        try {
            mockMvc.perform(
                    get("/api/v1/organizations/{id}", organizationId)
                            .header("Authorization", bearer(viewerToken))
            ).andExpect(status().isForbidden());
        } finally {
            membership.setStatus(MembershipStatus.ACTIVE);
            membershipRepository.saveAndFlush(membership);
        }
    }

    private String registerAndLogin(String email) throws Exception {

        mockMvc.perform(
                post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of(
                                        "fullName", "Integration Test User",
                                        "email", email,
                                        "password", "TestPassword123"
                                )
                        ))
        ).andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(
                post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of(
                                        "email", email,
                                        "password", "TestPassword123"
                                )
                        ))
        ).andExpect(status().isOk()).andReturn();

        return objectMapper.readTree(
                loginResult.getResponse().getContentAsString()
        ).get("accessToken").asText();
    }

    @Test
    void shouldRejectAccessWhenOrganizationIsSuspended() throws Exception {

        Organization organization = organizationRepository
                .findById(organizationId)
                .orElseThrow();

        organization.setStatus(OrganizationStatus.SUSPENDED);
        organizationRepository.saveAndFlush(organization);

        try {
            mockMvc.perform(
                    get("/api/v1/organizations/{id}", organizationId)
                            .header("Authorization", bearer(ownerToken))
            ).andExpect(status().isForbidden());
        } finally {
            organization.setStatus(OrganizationStatus.ACTIVE);
            organizationRepository.saveAndFlush(organization);
        }
    }

    private void addMembership(
            String email,
            OrganizationRole role
    ) {
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow();

        Organization organization = organizationRepository
                .findById(organizationId)
                .orElseThrow();

        OrganizationMembership membership = new OrganizationMembership();
        membership.setUser(user);
        membership.setOrganization(organization);
        membership.setRole(role);

        membershipRepository.saveAndFlush(membership);
    }

    private void assertCanView(String token) throws Exception {
        mockMvc.perform(
                get("/api/v1/organizations/{id}", organizationId)
                        .header("Authorization", bearer(token))
        ).andExpect(status().isOk());
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}
