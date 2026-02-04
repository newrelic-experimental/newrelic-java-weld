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
}
