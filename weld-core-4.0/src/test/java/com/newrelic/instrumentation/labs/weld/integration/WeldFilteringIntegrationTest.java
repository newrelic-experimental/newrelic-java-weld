package com.newrelic.instrumentation.labs.weld.integration;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import com.newrelic.agent.introspec.Introspector;
import com.newrelic.agent.introspec.TracedMetricData;
import com.newrelic.agent.introspec.TransactionTrace;
import com.newrelic.weld.test.web.Game;
import com.newrelic.weld.test.web.Generator;
import com.newrelic.weld.test.web.MaxNumber;
import org.jboss.weld.environment.se.Weld;
import org.jboss.weld.environment.se.WeldContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * Integration tests for Weld instrumentation filtering.
 * Tests BeanInstance and ProxyCall filtering with beaninstance_whitelist configuration.
 *
 * Note: For multiple configurations, create separate test classes with different configName values.
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(includePrefixes = {"org.jboss.weld", "com.newrelic.weld.test"},
                           configName = "beaninstance_whitelist.yml")
public class WeldFilteringIntegrationTest {

    private static WeldContainer weldContainer;
    private static Game game;

    @BeforeClass
    public static void setup() {
        // Initialize Weld CDI container
        Weld weld = new Weld();
        weldContainer = weld.initialize();

        // Get CDI bean (will be a proxy)
        game = weldContainer.select(Game.class).get();

        assertNotNull("Game bean should be injected", game);
        assertTrue("Game should be a Weld proxy",
            game.getClass().getName().contains("Proxy"));
    }

    @AfterClass
    public static void teardown() {
        if (weldContainer != null) {
            weldContainer.shutdown();
        }
    }

    /**
     * Test 1: BeanInstance Whitelist - Only Specified Methods Traced
     * Uses: beaninstance_whitelist.yml (configured at class level)
     */
    @Test
    public void testBeanInstanceWhitelist_OnlyMatchingMethodsTraced() {
        // Invoke multiple methods
        // Invoke multiple methods
        game.setGuess(50);        // NOT in whitelist
        boolean result = game.check();  // IN whitelist (full name)
        game.reset();             // IN whitelist (full name)
        String status = game.getGameStatus();  // IN whitelist (regex: get.*)
        int remaining = game.getRemainingGuesses();  // IN whitelist (regex: get.*)
        int number = game.getNumber();  // IN whitelist (regex: get.*)
        boolean won = game.isGameWon();  // NOT in whitelist
        boolean lost = game.isGameLost();  // NOT in whitelist

        Introspector introspector = InstrumentationTestRunner.getIntrospector();
        Map<String, TracedMetricData> unscopedMetrics = introspector.getUnscopedMetrics();

        // SHOULD be traced (in whitelist)
        assertTrue("Should trace check (full name match)",
            hasMetricContaining(unscopedMetrics, "Custom/Weld/BeanInstance", "check"));
        assertTrue("Should trace reset (full name match)",
            hasMetricContaining(unscopedMetrics, "Custom/Weld/BeanInstance", "reset"));
        assertTrue("Should trace getGameStatus (regex match)",
            hasMetricContaining(unscopedMetrics, "Custom/Weld/BeanInstance", "getGameStatus"));
        assertTrue("Should trace getRemainingGuesses (regex match)",
            hasMetricContaining(unscopedMetrics, "Custom/Weld/BeanInstance", "getRemainingGuesses"));
        assertTrue("Should trace getNumber (regex match)",
            hasMetricContaining(unscopedMetrics, "Custom/Weld/BeanInstance", "getNumber"));

        // SHOULD NOT be traced (not in whitelist)
        assertFalse("Should NOT trace setGuess (not whitelisted)",
            hasMetricContaining(unscopedMetrics, "Custom/Weld/BeanInstance", "setGuess"));
        assertFalse("Should NOT trace isGameWon (not whitelisted)",
            hasMetricContaining(unscopedMetrics, "Custom/Weld/BeanInstance", "isGameWon"));
        assertFalse("Should NOT trace isGameLost (not whitelisted)",
            hasMetricContaining(unscopedMetrics, "Custom/Weld/BeanInstance", "isGameLost"));
    }

    /**
     * Test 2: Verify Transaction Trace Contains Filtered Segments
     * Uses: beaninstance_whitelist.yml (configured at class level)
     */
    @Test
    public void testTransactionTrace_ContainsFilteredSegments() {
        // Invoke methods in a transaction
        game.setGuess(50);  // Filtered out
        game.check();       // Traced
        game.getGameStatus();  // Traced

        Introspector introspector = InstrumentationTestRunner.getIntrospector();

        // Get transaction traces
        Collection<TransactionTrace> traces = introspector.getTransactionTracesForTransaction(
            introspector.getTransactionNames().iterator().next());

        assertFalse("Should have transaction traces", traces.isEmpty());

        TransactionTrace trace = traces.iterator().next();

        // Verify segments in trace contain only whitelisted methods
        assertTrue("Trace should contain check segment",
            traceContainsSegment(trace, "BeanInstance", "check"));
        assertTrue("Trace should contain getGameStatus segment",
            traceContainsSegment(trace, "BeanInstance", "getGameStatus"));

        // setGuess should not appear (filtered out)
        assertFalse("Trace should NOT contain setGuess segment",
            traceContainsSegment(trace, "BeanInstance", "setGuess"));
    }

    /**
     * Helper: Check if metrics map contains a metric with specified substrings
     */
    private boolean hasMetricContaining(Map<String, TracedMetricData> metrics,
                                        String... substrings) {
        for (String metricName : metrics.keySet()) {
            boolean matchesAll = true;
            for (String substring : substrings) {
                if (!metricName.contains(substring)) {
                    matchesAll = false;
                    break;
                }
            }
            if (matchesAll) {
                return true;
            }
        }
        return false;
    }

    /**
     * Helper: Check if transaction trace contains a segment with specified substrings
     */
    private boolean traceContainsSegment(TransactionTrace trace, String... substrings) {
        return segmentContains(trace.getInitialTraceSegment(), substrings);
    }

    private boolean segmentContains(com.newrelic.agent.introspec.TraceSegment segment,
                                     String... substrings) {
        String segmentName = segment.getName();
        boolean matches = true;
        for (String substring : substrings) {
            if (!segmentName.contains(substring)) {
                matches = false;
                break;
            }
        }
        if (matches) {
            return true;
        }

        // Check children recursively
        for (com.newrelic.agent.introspec.TraceSegment child : segment.getChildren()) {
            if (segmentContains(child, substrings)) {
                return true;
            }
        }
        return false;
    }
}
