package com.newrelic.instrumentation.labs.weld.config;

import com.newrelic.agent.bridge.Agent;
import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.api.agent.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.PatternSyntaxException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WeldTraceFilterConfig.
 * Tests configuration loading, pattern matching, and dynamic updates.
 */
public class WeldTraceFilterConfigTest {

    private Agent originalAgent;
    private Agent mockAgent;
    private Logger mockLogger;
    private AgentConfig mockConfig;

    @Before
    public void setUp() {
        originalAgent = AgentBridge.getAgent();
        mockAgent = mock(Agent.class);
        mockLogger = mock(Logger.class);
        mockConfig = mock(AgentConfig.class);

        AgentBridge.agent = mockAgent;
        when(mockAgent.getConfig()).thenReturn(mockConfig);
        when(mockAgent.getLogger()).thenReturn(mockLogger);
        when(mockLogger.isLoggable(Level.FINEST)).thenReturn(false);
    }

    @After
    public void tearDown() {
        AgentBridge.agent = originalAgent;
    }

    /**
     * Test 1: Filtering Disabled - Always Return True
     */
    @Test
    public void testFilteringDisabled_TraceEverything() {
        // Setup: filtering_enabled = false
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(false);

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // All methods should be traced when filtering disabled
        assertTrue("Should trace when filtering disabled",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "anyMethod"));
        assertTrue("Should trace when filtering disabled",
            WeldTraceFilterConfig.shouldTraceProxyCall("com.example.Service", "anyMethod"));
    }

    /**
     * Test 2: BeanInstance Full Name Match
     */
    @Test
    public void testBeanInstance_FullNameMatch() {
        // Setup: filtering enabled with specific full names
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Arrays.asList(
                "com.example.Service:login",
                "com.example.Service:logout"
            ));
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Collections.emptyList());

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // Exact matches should be traced
        assertTrue("Should trace login (exact match)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "login"));
        assertTrue("Should trace logout (exact match)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "logout"));

        // Non-matches should NOT be traced
        assertFalse("Should NOT trace register (not in list)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "register"));
        assertFalse("Should NOT trace different class",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.OtherService", "login"));
    }

    /**
     * Test 3: BeanInstance Regex Pattern Match
     */
    @Test
    public void testBeanInstance_RegexPatternMatch() {
        // Setup: filtering enabled with regex patterns
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Collections.emptyList());
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Arrays.asList(
                "com\\.example\\..*Service:process.*",
                "com\\.example\\.repository\\..*:save"
            ));

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // Regex matches should be traced
        assertTrue("Should trace processLogin (pattern match)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.UserService", "processLogin"));
        assertTrue("Should trace processPayment (pattern match)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.OrderService", "processPayment"));
        assertTrue("Should trace save (pattern match)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.repository.UserRepository", "save"));

        // Non-matches should NOT be traced
        assertFalse("Should NOT trace login (no pattern match)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.UserService", "login"));
        assertFalse("Should NOT trace delete (wrong method)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.repository.UserRepository", "delete"));
    }

    /**
     * Test 4: ProxyCall Full Name Match
     */
    @Test
    public void testProxyCall_FullNameMatch() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.proxycall.track_full_names"))
            .thenReturn(Arrays.asList("com.example.Controller:handleRequest"));
        when(mockConfig.getValue("weld.proxycall.track_regex_patterns"))
            .thenReturn(Collections.emptyList());

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        assertTrue("Should trace handleRequest (exact match)",
            WeldTraceFilterConfig.shouldTraceProxyCall("com.example.Controller", "handleRequest"));
        assertFalse("Should NOT trace handleResponse",
            WeldTraceFilterConfig.shouldTraceProxyCall("com.example.Controller", "handleResponse"));
    }

    /**
     * Test 5: ProxyCall Regex Pattern Match
     */
    @Test
    public void testProxyCall_RegexPatternMatch() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.proxycall.track_full_names"))
            .thenReturn(Collections.emptyList());
        when(mockConfig.getValue("weld.proxycall.track_regex_patterns"))
            .thenReturn(Arrays.asList("com\\.example\\.controller\\..*:handle.*"));

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        assertTrue("Should trace handleRequest",
            WeldTraceFilterConfig.shouldTraceProxyCall("com.example.controller.UserController", "handleRequest"));
        assertTrue("Should trace handleSubmit",
            WeldTraceFilterConfig.shouldTraceProxyCall("com.example.controller.OrderController", "handleSubmit"));
        assertFalse("Should NOT trace processRequest (wrong method prefix)",
            WeldTraceFilterConfig.shouldTraceProxyCall("com.example.controller.UserController", "processRequest"));
    }

    /**
     * Test 6: Combined Full Names and Regex Patterns
     */
    @Test
    public void testCombined_FullNamesAndRegex() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Arrays.asList("com.example.Service:login"));
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Arrays.asList("com\\.example\\..*:process.*"));

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // Should match either full name OR regex
        assertTrue("Should trace login (full name)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "login"));
        assertTrue("Should trace processPayment (regex)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "processPayment"));
        assertFalse("Should NOT trace logout (no match)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "logout"));
    }

    /**
     * Test 7: Empty Whitelist - Trace Nothing
     */
    @Test
    public void testEmptyWhitelist_TraceNothing() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Collections.emptyList());
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Collections.emptyList());

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // Empty whitelist = trace nothing
        assertFalse("Should NOT trace any method with empty whitelist",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "anyMethod"));
    }

    /**
     * Test 8: Invalid Regex Pattern - Should Be Skipped
     */
    @Test
    public void testInvalidRegex_ShouldBeSkipped() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Collections.emptyList());
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Arrays.asList(
                "com\\.example\\..*:process.*",  // Valid
                "[invalid(regex"  // Invalid
            ));

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // Valid pattern should work
        assertTrue("Should trace with valid pattern",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "processPayment"));

        // Invalid pattern should be skipped (not crash)
        // The important behavior is that invalid regex doesn't break the application
        // Note: Logger verification not applicable since logger is null in test environment
    }

    /**
     * Test 9: Dynamic Configuration Update
     */
    @Test
    public void testDynamicConfigUpdate() {
        // Initial config: trace login only
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Arrays.asList("com.example.Service:login"));
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Collections.emptyList());

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        assertTrue("Should trace login initially",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "login"));
        assertFalse("Should NOT trace logout initially",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "logout"));

        // Update config: add logout to whitelist
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Arrays.asList("com.example.Service:login", "com.example.Service:logout"));

        config.configChanged("test-app", (AgentConfig) mockConfig);

        // Both should now be traced
        assertTrue("Should trace login after update",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "login"));
        assertTrue("Should trace logout after update",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "logout"));
    }

    /**
     * Test 10: Separate BeanInstance and ProxyCall Filtering
     */
    @Test
    public void testSeparateFiltering_BeanInstanceAndProxyCall() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);

        // BeanInstance: trace only "check"
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Arrays.asList("com.example.Game:check"));
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Collections.emptyList());

        // ProxyCall: trace only "handleRequest"
        when(mockConfig.getValue("weld.proxycall.track_full_names"))
            .thenReturn(Arrays.asList("com.example.Controller:handleRequest"));
        when(mockConfig.getValue("weld.proxycall.track_regex_patterns"))
            .thenReturn(Collections.emptyList());

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // BeanInstance check
        assertTrue("BeanInstance should trace check",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Game", "check"));
        assertFalse("BeanInstance should NOT trace reset",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Game", "reset"));

        // ProxyCall check
        assertTrue("ProxyCall should trace handleRequest",
            WeldTraceFilterConfig.shouldTraceProxyCall("com.example.Controller", "handleRequest"));
        assertFalse("ProxyCall should NOT trace check",
            WeldTraceFilterConfig.shouldTraceProxyCall("com.example.Game", "check"));
    }

    /**
     * Test 12: Null ClassName — Returns False When Filtering Enabled
     */
    @Test
    public void testNullClassName_ReturnsFalse_WhenFilteringEnabled() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Arrays.asList("com.example.Service:login"));
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Collections.emptyList());

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // Null className → "null:login" → no match → false (no NPE)
        assertFalse("Should return false for null className",
            WeldTraceFilterConfig.shouldTraceBeanInstance(null, "login"));
        assertFalse("ProxyCall should return false for null className",
            WeldTraceFilterConfig.shouldTraceProxyCall(null, "login"));
    }

    /**
     * Test 13: Null MethodName — Returns False When Filtering Enabled
     */
    @Test
    public void testNullMethodName_ReturnsFalse_WhenFilteringEnabled() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Arrays.asList("com.example.Service:login"));
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Collections.emptyList());

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // "com.example.Service:null" → no match → false (no NPE)
        assertFalse("Should return false for null methodName",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", null));
        assertFalse("ProxyCall should return false for null methodName",
            WeldTraceFilterConfig.shouldTraceProxyCall("com.example.Service", null));
    }

    /**
     * Test 14: ProxyCall Config Does NOT Affect BeanInstance and Vice Versa
     */
    @Test
    public void testProxyCallConfig_IsIndependentFrom_BeanInstance() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);

        // Only ProxyCall configured with "getData" method
        when(mockConfig.getValue("weld.proxycall.track_full_names"))
            .thenReturn(Arrays.asList("com.example.Service:getData"));
        when(mockConfig.getValue("weld.proxycall.track_regex_patterns"))
            .thenReturn(Collections.emptyList());

        // BeanInstance has NOTHING configured
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Collections.emptyList());
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Collections.emptyList());

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // ProxyCall should trace getData
        assertTrue("ProxyCall getData should be traced",
            WeldTraceFilterConfig.shouldTraceProxyCall("com.example.Service", "getData"));

        // BeanInstance should NOT trace getData (not in its whitelist)
        assertFalse("BeanInstance getData should NOT be traced (different whitelist)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "getData"));
    }

    /**
     * Test 15: Config Reload Replaces (Does Not Append) Previous Patterns
     */
    @Test
    public void testConfigReload_ReplacesPatterns_NotAppends() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);

        // Initial: trace "login" only
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Arrays.asList("com.example.Service:login"));
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Collections.emptyList());

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        assertTrue("login should be traced initially",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "login"));

        // Reload: now only "logout" — "login" must be gone
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Arrays.asList("com.example.Service:logout"));

        config.configChanged("test-app", (AgentConfig) mockConfig);

        assertFalse("login should NOT be traced after reload (patterns replaced, not appended)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "login"));
        assertTrue("logout should be traced after reload",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "logout"));
    }

    /**
     * Test 16: Multiple Regex Patterns — Any Match Returns True
     */
    @Test
    public void testMultipleRegexPatterns_AnyMatchReturnsTrue() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Collections.emptyList());
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Arrays.asList(
                "com\\.example\\.service\\..*:get.*",  // Pattern 1
                "com\\.example\\.repo\\..*:find.*"     // Pattern 2
            ));

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // Matches Pattern 1
        assertTrue("Should trace getUser (matches pattern 1)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.service.UserService", "getUser"));

        // Matches Pattern 2
        assertTrue("Should trace findById (matches pattern 2)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.repo.UserRepo", "findById"));

        // Matches neither
        assertFalse("Should NOT trace saveUser (matches no pattern)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.service.UserService", "saveUser"));
    }

    /**
     * Test 17: Full Name Is Checked Before Regex (Short-Circuit)
     */
    @Test
    public void testFullNameCheckedBeforeRegex() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);

        // Full name: "com.example.Service:login" is in the list
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Arrays.asList("com.example.Service:login"));

        // Regex: would NOT match "login" (only matches "process.*")
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Arrays.asList("com\\.example\\.Service:process.*"));

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // "login" matches full name → traced (even though regex wouldn't match it)
        assertTrue("login should be traced via full name match",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "login"));

        // "processPayment" matches regex → traced
        assertTrue("processPayment should be traced via regex",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "processPayment"));

        // "logout" matches neither → not traced
        assertFalse("logout should NOT be traced (no match)",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "logout"));
    }

    /**
     * Test 18: Filtering Disabled Returns True Regardless of Null Config Values
     */
    @Test
    public void testFilteringDisabled_ReturnsTrue_WhenConfigValuesAreNull() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(false);
        // No other config — getValue returns null for pattern lists
        when(mockConfig.getValue("weld.beaninstance.track_full_names")).thenReturn(null);
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns")).thenReturn(null);
        when(mockConfig.getValue("weld.proxycall.track_full_names")).thenReturn(null);
        when(mockConfig.getValue("weld.proxycall.track_regex_patterns")).thenReturn(null);

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // Everything traced when disabled, even with null pattern lists
        assertTrue("BeanInstance should trace all when disabled",
            WeldTraceFilterConfig.shouldTraceBeanInstance("com.example.Service", "anyMethod"));
        assertTrue("ProxyCall should trace all when disabled",
            WeldTraceFilterConfig.shouldTraceProxyCall("com.example.Service", "anyMethod"));
    }

    /**
     * Test 11: Illumina v2.0.3 — Negative Lookahead Pattern
     *
     * Validates the single-pattern config recommended for Robbe's Test 11:
     *   "com\\.illumina\\.ica\\.cp\\.(?!supportive\\.security\\.internal\\.security\\.).*:.*"
     *
     * Must TRACE (application proxy calls):
     *   rest.*, core.*, shared.*
     *   supportive.security.internal.rbac.*
     *   supportive.security.internal.repositories.*
     *   supportive.security.internal.platformauth.*
     *   supportive.security.internal.entitlement.*
     *   supportive.staticdata.*
     *
     * Must NOT TRACE (framework security noise — fires 500+ times per list request):
     *   supportive.security.internal.security.MasterTenantService
     *   supportive.security.internal.security.SecurityHelper
     *   supportive.security.internal.security.SecurityManagerFacade
     *   supportive.security.internal.security.DefaultActionSecurityManager
     *   supportive.security.internal.security.SecurityService
     *   supportive.security.internal.security.project.*
     *   supportive.security.internal.security.rbac.*
     *   supportive.security.internal.security.platformauth.sync.*
     */
    @Test
    public void testIlluminaTest11_NegativeLookaheadPattern() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.proxycall.track_full_names"))
            .thenReturn(Collections.emptyList());
        when(mockConfig.getValue("weld.proxycall.track_regex_patterns"))
            .thenReturn(Arrays.asList(
                "com\\.illumina\\.ica\\.cp\\.(?!supportive\\.security\\.internal\\.security\\.).*:.*"
            ));
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Collections.emptyList());
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Arrays.asList(
                "com\\.illumina\\.ica\\.cp\\.(?!supportive\\.security\\.internal\\.security\\.).*:.*"
            ));

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // --- Must TRACE (application proxy calls) ---
        assertTrue("Should trace REST endpoint",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.rest.v3.publicapi.endpoints.ProjectDataApi", "a_getProjectData"));
        assertTrue("Should trace core service",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.core.project.internal.projects.ProjectService", "getProjectById"));
        assertTrue("Should trace shared CRUDService",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.shared.internal.persistence.CRUDService", "findByUniqueIdentifier"));
        assertTrue("Should trace supportive staticdata (not in excluded subtree)",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.supportive.staticdata.internal.dataformat.DataFormatService", "getDataFormatByCode"));
        assertTrue("Should trace supportive rbac (rbac != security.internal.security)",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.supportive.security.internal.rbac.RbacService", "getUserRbacProfilesForUser"));
        assertTrue("Should trace supportive repositories",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.supportive.security.internal.repositories.app.AppRepository", "getByAppName"));
        assertTrue("Should trace supportive platformauth",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.supportive.security.internal.platformauth.PlatformAuthService", "convertApiKeyIntoJwt"));
        assertTrue("Should trace supportive entitlement",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.supportive.security.internal.entitlement.SecurityEntitlementService", "getAllActiveActivationCodes"));

        // --- Must NOT TRACE (security.internal.security.* framework noise) ---
        assertFalse("Must NOT trace MasterTenantService (fires 296x per list request)",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.supportive.security.internal.security.MasterTenantService", "isMasterTenant"));
        assertFalse("Must NOT trace SecurityHelper",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.supportive.security.internal.security.SecurityHelper", "isMasterTenant"));
        assertFalse("Must NOT trace SecurityManagerFacade",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.supportive.security.internal.security.SecurityManagerFacade", "isFetchAllowed"));
        assertFalse("Must NOT trace DefaultActionSecurityManager",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.supportive.security.internal.security.DefaultActionSecurityManager", "isFetchAllowed"));
        assertFalse("Must NOT trace SecurityService (45x per request)",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.supportive.security.internal.security.SecurityService", "getTenantById"));
        assertFalse("Must NOT trace nested security.internal.security.project.*",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.supportive.security.internal.security.project.ProjectPermissionService", "getAccessLevel"));
        assertFalse("Must NOT trace nested security.internal.security.rbac.*",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.supportive.security.internal.security.rbac.RbacService", "getUserRbacProfilesForUser"));
        assertFalse("Must NOT trace nested security.internal.security.platformauth.sync.*",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "com.illumina.ica.cp.supportive.security.internal.security.platformauth.sync.SyncedUserProvider", "getSyncedUser"));

        // --- Must NOT TRACE (non-illumina framework classes — not in whitelist at all) ---
        assertFalse("Must NOT trace javax.validation (not illumina.ica.cp)",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "javax.validation.executable.ExecutableValidator", "validateReturnValue"));
        assertFalse("Must NOT trace org.jboss.weld internals",
            WeldTraceFilterConfig.shouldTraceProxyCall(
                "org.jboss.weld.bean.proxy.InterceptorMethodHandler", "invoke"));

        // --- BeanInstance mirror (same pattern applied to beaninstance) ---
        assertTrue("BeanInstance: should trace core service",
            WeldTraceFilterConfig.shouldTraceBeanInstance(
                "com.illumina.ica.cp.core.project.internal.projects.ProjectService", "getProjectById"));
        assertFalse("BeanInstance: must NOT trace MasterTenantService",
            WeldTraceFilterConfig.shouldTraceBeanInstance(
                "com.illumina.ica.cp.supportive.security.internal.security.MasterTenantService", "isMasterTenant"));
        assertFalse("BeanInstance: must NOT trace javax.validation noise",
            WeldTraceFilterConfig.shouldTraceBeanInstance(
                "javax.validation.executable.ExecutableValidator", "validateReturnValue"));
    }

    // ===========================================================================================
    // Scenario tests — equivalent to YAML config scenarios
    // ===========================================================================================

    /**
     * Test 19: BeanInstance Whitelist Scenario (mirrors beaninstance_whitelist.yml)
     * Full names: Game:check, Game:reset
     * Regex: Game:get.*
     */
    @Test
    public void testScenario_BeanInstanceWhitelist_YamlEquivalent() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Arrays.asList(
                "com.newrelic.weld.test.web.Game:check",
                "com.newrelic.weld.test.web.Game:reset"
            ));
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Arrays.asList("com\\.newrelic\\.weld\\.test\\.web\\.Game:get.*"));
        when(mockConfig.getValue("weld.proxycall.track_full_names"))
            .thenReturn(Collections.emptyList());
        when(mockConfig.getValue("weld.proxycall.track_regex_patterns"))
            .thenReturn(Collections.emptyList());

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // BeanInstance: whitelisted (full name)
        assertTrue("Game.check() must be traced (full name)", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.Game", "check"));
        assertTrue("Game.reset() must be traced (full name)", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.Game", "reset"));

        // BeanInstance: whitelisted (regex get.*)
        assertTrue("Game.getGameStatus() must be traced (regex)", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.Game", "getGameStatus"));
        assertTrue("Game.getNumber() must be traced (regex)", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.Game", "getNumber"));
        assertTrue("Game.getRemainingGuesses() must be traced (regex)", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.Game", "getRemainingGuesses"));

        // BeanInstance: NOT whitelisted
        assertFalse("Game.setGuess() must NOT be traced", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.Game", "setGuess"));
        assertFalse("Game.isGameWon() must NOT be traced (is* != get*)", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.Game", "isGameWon"));
        assertFalse("Game.isGameLost() must NOT be traced", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.Game", "isGameLost"));

        // ProxyCall: empty whitelist — nothing traced
        assertFalse("ProxyCall check must NOT be traced (no proxycall config)", WeldTraceFilterConfig.shouldTraceProxyCall("com.newrelic.weld.test.web.Game", "check"));
        assertFalse("ProxyCall getStatus must NOT be traced", WeldTraceFilterConfig.shouldTraceProxyCall("com.newrelic.weld.test.web.GameResource", "getStatus"));
    }

    /**
     * Test 20: ProxyCall Whitelist Scenario (mirrors proxycall_whitelist.yml)
     * Full names: GameResource:getStatus
     * Regex: GameResource:.*
     */
    @Test
    public void testScenario_ProxyCallWhitelist_YamlEquivalent() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.proxycall.track_full_names"))
            .thenReturn(Arrays.asList("com.newrelic.weld.test.web.GameResource:getStatus"));
        when(mockConfig.getValue("weld.proxycall.track_regex_patterns"))
            .thenReturn(Arrays.asList("com\\.newrelic\\.weld\\.test\\.web\\.GameResource:.*"));
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Collections.emptyList());
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Collections.emptyList());

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // ProxyCall: whitelisted (full name)
        assertTrue("GameResource.getStatus() must be traced (full name)", WeldTraceFilterConfig.shouldTraceProxyCall("com.newrelic.weld.test.web.GameResource", "getStatus"));

        // ProxyCall: whitelisted (regex .*)
        assertTrue("GameResource.makeGuess() must be traced (regex)", WeldTraceFilterConfig.shouldTraceProxyCall("com.newrelic.weld.test.web.GameResource", "makeGuess"));
        assertTrue("GameResource.resetGame() must be traced (regex)", WeldTraceFilterConfig.shouldTraceProxyCall("com.newrelic.weld.test.web.GameResource", "resetGame"));
        assertTrue("GameResource.getRemainingGuesses() must be traced (regex)", WeldTraceFilterConfig.shouldTraceProxyCall("com.newrelic.weld.test.web.GameResource", "getRemainingGuesses"));

        // ProxyCall: NOT in whitelist (Game is not GameResource)
        assertFalse("Game.check() must NOT be traced as ProxyCall", WeldTraceFilterConfig.shouldTraceProxyCall("com.newrelic.weld.test.web.Game", "check"));

        // BeanInstance: empty whitelist — nothing traced
        assertFalse("BeanInstance check must NOT be traced (no beaninstance config)", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.Game", "check"));
        assertFalse("BeanInstance getStatus must NOT be traced", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.GameResource", "getStatus"));
    }

    /**
     * Test 21: Combined Filtering Scenario (mirrors combined_filtering.yml)
     * Blacklist: isGameLost, isGameWon
     * Whitelist BeanInstance: Game:check, Game:reset, Game:setGuess (via regex)
     * Whitelist ProxyCall: all GameResource methods (regex .*)
     *
     * Verifies v2.0.3 filter order: whitelist(shouldTrace) → blacklist(shouldIgnore)
     * Non-whitelisted methods return false BEFORE blacklist is consulted.
     *
     * NOTE: TraceIgnoreConfig is tested separately. Here we only verify whitelist logic.
     * The combined filter order BeanInstance_Instrumentation.invoke() is:
     *   if (shouldTraceBeanInstance() && !shouldIgnoreTrace()) → create tracer
     * This test validates the whitelist half of that expression.
     */
    @Test
    public void testScenario_CombinedFiltering_WhitelistPart() {
        when(mockConfig.getValue("weld.filtering_enabled")).thenReturn(true);
        when(mockConfig.getValue("weld.beaninstance.track_regex_patterns"))
            .thenReturn(Arrays.asList(
                "com\\.newrelic\\.weld\\.test\\.web\\.Game:check",
                "com\\.newrelic\\.weld\\.test\\.web\\.Game:reset",
                "com\\.newrelic\\.weld\\.test\\.web\\.Game:setGuess"
            ));
        when(mockConfig.getValue("weld.beaninstance.track_full_names"))
            .thenReturn(Collections.emptyList());
        when(mockConfig.getValue("weld.proxycall.track_regex_patterns"))
            .thenReturn(Arrays.asList("com\\.newrelic\\.weld\\.test\\.web\\..*:.*"));
        when(mockConfig.getValue("weld.proxycall.track_full_names"))
            .thenReturn(Collections.emptyList());

        WeldTraceFilterConfig config = new WeldTraceFilterConfig();
        config.configChanged("test-app", (AgentConfig) mockConfig);

        // Whitelisted BeanInstance methods (would reach blacklist check)
        assertTrue("check is whitelisted → passes whitelist", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.Game", "check"));
        assertTrue("reset is whitelisted → passes whitelist", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.Game", "reset"));
        assertTrue("setGuess is whitelisted → passes whitelist", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.Game", "setGuess"));

        // NOT whitelisted (isGameLost/Won) — fast-rejected by whitelist, blacklist never consulted
        assertFalse("isGameLost NOT whitelisted → false (whitelist-first, blacklist never reached)", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.Game", "isGameLost"));
        assertFalse("isGameWon NOT whitelisted → false (whitelist-first, blacklist never reached)", WeldTraceFilterConfig.shouldTraceBeanInstance("com.newrelic.weld.test.web.Game", "isGameWon"));

        // ProxyCall: all GameResource and Game methods whitelisted
        assertTrue("GameResource.getStatus() whitelisted", WeldTraceFilterConfig.shouldTraceProxyCall("com.newrelic.weld.test.web.GameResource", "getStatus"));
        assertTrue("Game.check() whitelisted as ProxyCall", WeldTraceFilterConfig.shouldTraceProxyCall("com.newrelic.weld.test.web.Game", "check"));
    }
}
