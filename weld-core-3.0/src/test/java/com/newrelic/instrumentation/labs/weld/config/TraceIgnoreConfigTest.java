package com.newrelic.instrumentation.labs.weld.config;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/**
 * Tests for TraceIgnoreConfig (blacklist / ignore-traces).
 *
 * Uses InstrumentationTestRunner so the NR agent is fully initialized:
 *   - ServiceFactory.getConfigService() is available when TraceIgnoreConfig's static block runs
 *   - The YAML config is loaded and passed to configChanged() before any test executes
 *
 * Config: ignore_traces_wildcard.yml
 *   ignore_traces_enabled: true
 *   ignored_trace_patterns:
 *     - "com.newrelic.weld.test.web.Game:isGameLost"   (exact match)
 *     - "com.newrelic.weld.test.web.Game:isGameWon"    (exact match)
 *     - "javax.validation.*:*"                          (wildcard — matches any javax.validation class + method)
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(
    includePrefixes = {"com.newrelic.instrumentation.labs.weld.config"},
    configName = "ignore_traces_wildcard.yml"
)
public class TraceIgnoreConfigTest {

    // --- Exact match tests ---

    /**
     * Exact pattern "com.newrelic.weld.test.web.Game:isGameLost" must be ignored.
     */
    @Test
    public void testExactMatch_isGameLost_IsIgnored() {
        assertTrue("isGameLost must be blacklisted (exact pattern match)",
            TraceIgnoreConfig.shouldIgnoreTrace("com.newrelic.weld.test.web.Game:isGameLost"));
    }

    /**
     * Exact pattern "com.newrelic.weld.test.web.Game:isGameWon" must be ignored.
     */
    @Test
    public void testExactMatch_isGameWon_IsIgnored() {
        assertTrue("isGameWon must be blacklisted (exact pattern match)",
            TraceIgnoreConfig.shouldIgnoreTrace("com.newrelic.weld.test.web.Game:isGameWon"));
    }

    /**
     * "check" is not in any ignore pattern → must NOT be ignored.
     */
    @Test
    public void testExactMatch_check_IsNotIgnored() {
        assertFalse("check must NOT be blacklisted",
            TraceIgnoreConfig.shouldIgnoreTrace("com.newrelic.weld.test.web.Game:check"));
    }

    /**
     * "reset" is not in any ignore pattern → must NOT be ignored.
     */
    @Test
    public void testExactMatch_reset_IsNotIgnored() {
        assertFalse("reset must NOT be blacklisted",
            TraceIgnoreConfig.shouldIgnoreTrace("com.newrelic.weld.test.web.Game:reset"));
    }

    /**
     * Same method name on a DIFFERENT class is not ignored (exact match requires full class).
     */
    @Test
    public void testExactMatch_SameMethodDifferentClass_IsNotIgnored() {
        assertFalse("isGameLost on a different class should NOT be ignored",
            TraceIgnoreConfig.shouldIgnoreTrace("com.example.OtherGame:isGameLost"));
    }

    // --- Wildcard pattern tests ---

    /**
     * Wildcard "javax.validation.*:*" matches any javax.validation class and method.
     */
    @Test
    public void testWildcard_JavaxValidationExecutableValidator_IsIgnored() {
        assertTrue("javax.validation.executable.ExecutableValidator:validateParameters must be ignored",
            TraceIgnoreConfig.shouldIgnoreTrace(
                "javax.validation.executable.ExecutableValidator:validateParameters"));
    }

    /**
     * Wildcard matches deeply nested sub-packages under javax.validation.
     */
    @Test
    public void testWildcard_JavaxValidationNestedPackage_IsIgnored() {
        assertTrue("javax.validation.constraints.FooValidator:initialize must be ignored",
            TraceIgnoreConfig.shouldIgnoreTrace(
                "javax.validation.constraints.FooValidator:initialize"));
    }

    /**
     * Wildcard for javax.validation does NOT match a different root package.
     */
    @Test
    public void testWildcard_DifferentRootPackage_IsNotIgnored() {
        assertFalse("jakarta.validation.* must NOT be ignored (different root package)",
            TraceIgnoreConfig.shouldIgnoreTrace(
                "jakarta.validation.executable.ExecutableValidator:validateParameters"));
    }

    /**
     * A completely unrelated class is never ignored.
     */
    @Test
    public void testUnrelatedClass_IsNotIgnored() {
        assertFalse("com.example.Service:doWork must NOT be ignored",
            TraceIgnoreConfig.shouldIgnoreTrace("com.example.Service:doWork"));
    }

    /**
     * Empty string does not match any pattern.
     */
    @Test
    public void testEmptyString_IsNotIgnored() {
        assertFalse("Empty string should not be ignored",
            TraceIgnoreConfig.shouldIgnoreTrace(""));
    }

    // --- Partial match sanity tests ---

    /**
     * A prefix match of an exact pattern (without the method) must NOT be ignored.
     * Patterns require full "class:method" form.
     */
    @Test
    public void testPartialMatch_ClassOnlyNoMethod_IsNotIgnored() {
        assertFalse("Class name without :method must NOT be ignored",
            TraceIgnoreConfig.shouldIgnoreTrace("com.newrelic.weld.test.web.Game"));
    }

    /**
     * javax.validation root itself (no sub-class) must not match wildcard "javax.validation.*:*".
     * The pattern requires at least one char after the dot before ":".
     */
    @Test
    public void testWildcard_RootPackageOnly_IsNotIgnored() {
        // "javax.validation:someMethod" has no sub-package — pattern requires "javax.validation." + something
        // convertWildcardToRegex("javax.validation.*:*") → \Qjavax.validation.\E.*\Q:\E.*
        // "javax.validation:someMethod" starts with "javax.validation" but NOT "javax.validation."
        assertFalse("javax.validation:someMethod should NOT match (no dot after validation)",
            TraceIgnoreConfig.shouldIgnoreTrace("javax.validation:someMethod"));
    }
}
