package com.newrelic.instrumentation.labs.weld.core_3;

import com.newrelic.agent.introspec.InstrumentationTestConfig;
import com.newrelic.agent.introspec.InstrumentationTestRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Unit tests for WeldCoreUtils utility methods.
 *
 * Tests two groups of functionality:
 *   1. cleanProxyClassName() — strips Weld-generated proxy suffixes from class names
 *   2. claimProxyCallTrace() / releaseProxyCallTrace() — per-thread deduplication of ProxyCall spans
 *
 * InstrumentationTestRunner is used so the NR agent is available for logging calls
 * inside cleanProxyClassName() when the class name is actually modified.
 */
@RunWith(InstrumentationTestRunner.class)
@InstrumentationTestConfig(
    includePrefixes = {"com.newrelic.instrumentation.labs.weld"},
    configName = "filtering_disabled.yml"
)
public class WeldCoreUtilsTest {

    /** Track signatures claimed in each test so @After can release them cleanly. */
    private final List<String> claimedSignatures = new ArrayList<>();

    @After
    public void releaseAllClaimed() {
        for (String sig : claimedSignatures) {
            WeldCoreUtils.releaseProxyCallTrace(sig);
        }
        claimedSignatures.clear();
    }

    // ===========================================================================================
    // cleanProxyClassName — Tests
    // ===========================================================================================

    /** Null input → null returned (no NPE). */
    @Test
    public void testClean_Null_ReturnsNull() {
        assertNull("null input should return null",
            WeldCoreUtils.cleanProxyClassName(null));
    }

    /** Empty string → empty string returned. */
    @Test
    public void testClean_EmptyString_ReturnsEmpty() {
        assertEquals("Empty string should return empty string",
            "", WeldCoreUtils.cleanProxyClassName(""));
    }

    /** Plain class name with no '$' → returned unchanged. */
    @Test
    public void testClean_PlainClassName_Unchanged() {
        String name = "com.example.Service";
        assertEquals("Plain class name should be unchanged",
            name, WeldCoreUtils.cleanProxyClassName(name));
    }

    /** Simple package + class, no proxy suffix → unchanged. */
    @Test
    public void testClean_SingleSegmentNoProxy_Unchanged() {
        assertEquals("MyService", WeldCoreUtils.cleanProxyClassName("MyService"));
    }

    /** Standard Weld client proxy suffix: $Proxy$_$$_WeldSubclass → stripped. */
    @Test
    public void testClean_WeldClientProxy_Stripped() {
        assertEquals(
            "com.example.Service",
            WeldCoreUtils.cleanProxyClassName("com.example.Service$Proxy$_$$_WeldSubclass")
        );
    }

    /** Weld target decorator proxy suffix: $Proxy$_$$_WeldTargetDecoratorSubclass → stripped. */
    @Test
    public void testClean_WeldTargetDecoratorProxy_Stripped() {
        assertEquals(
            "com.example.MyService",
            WeldCoreUtils.cleanProxyClassName(
                "com.example.MyService$Proxy$_$$_WeldTargetDecoratorSubclass")
        );
    }

    /** Realistic Illumina proxy class name (as seen in actual traces). */
    @Test
    public void testClean_RealisticIlluminaProxy_Stripped() {
        assertEquals(
            "com.illumina.ica.cp.core.project.ProjectService",
            WeldCoreUtils.cleanProxyClassName(
                "com.illumina.ica.cp.core.project.ProjectService$Proxy$_$$_WeldSubclass")
        );
    }

    /** '$' present but NO "Subclass" in suffix → returned unchanged. */
    @Test
    public void testClean_DollarButNoSubclass_Unchanged() {
        String name = "com.example.Service$InnerClass";
        assertEquals("'$' without Subclass suffix should be unchanged",
            name, WeldCoreUtils.cleanProxyClassName(name));
    }

    /** '$SomethingSubclass' at end (lowercase 's') → stripped because regex is case-sensitive
     *  and pattern is "\\$.*?Subclass" — capital 'S' required. */
    @Test
    public void testClean_LowercaseSubclass_Unchanged() {
        // PROXY_SUFFIX_REGEX = "\\$.*?Subclass" — note capital S
        // "$somethingsubclass" does NOT match → unchanged
        String name = "com.example.Service$somethingsubclass";
        assertEquals("Lowercase 'subclass' should NOT be stripped",
            name, WeldCoreUtils.cleanProxyClassName(name));
    }

    /** Stripping leaves no trailing '$'. */
    @Test
    public void testClean_ResultHasNoDollarTrailer() {
        String cleaned = WeldCoreUtils.cleanProxyClassName(
            "com.example.Service$Proxy$_$$_WeldSubclass");
        assertFalse("Cleaned name should not end with '$'", cleaned.endsWith("$"));
        assertFalse("Cleaned name should not contain '$'", cleaned.contains("$"));
    }

    // ===========================================================================================
    // claimProxyCallTrace / releaseProxyCallTrace — Thread-local dedup tests
    // ===========================================================================================

    /** First claim for a signature succeeds (returns true). */
    @Test
    public void testClaim_First_Succeeds() {
        String sig = "test.DedupClass:firstMethod";
        claimedSignatures.add(sig);
        assertTrue("First claim must return true", WeldCoreUtils.claimProxyCallTrace(sig));
    }

    /** Second claim for the SAME signature fails (returns false). */
    @Test
    public void testClaim_Duplicate_Fails() {
        String sig = "test.DedupClass:dupMethod";
        claimedSignatures.add(sig);
        WeldCoreUtils.claimProxyCallTrace(sig);  // first claim
        assertFalse("Second claim for same signature must return false",
            WeldCoreUtils.claimProxyCallTrace(sig));
    }

    /** After release, the same signature can be claimed again. */
    @Test
    public void testRelease_AllowsReclaim() {
        String sig = "test.DedupClass:reclaim";
        WeldCoreUtils.claimProxyCallTrace(sig);   // claim
        WeldCoreUtils.releaseProxyCallTrace(sig);  // release
        // Should succeed now
        boolean reClaimed = WeldCoreUtils.claimProxyCallTrace(sig);
        claimedSignatures.add(sig);  // ensure cleanup
        assertTrue("Should be claimable again after release", reClaimed);
    }

    /** Two different signatures are tracked independently. */
    @Test
    public void testDifferentSignatures_AreIndependent() {
        String sig1 = "test.DedupClass:method1";
        String sig2 = "test.DedupClass:method2";
        claimedSignatures.add(sig1);
        claimedSignatures.add(sig2);

        assertTrue("sig1 first claim must succeed", WeldCoreUtils.claimProxyCallTrace(sig1));
        assertTrue("sig2 first claim must succeed independently", WeldCoreUtils.claimProxyCallTrace(sig2));
        assertFalse("sig1 second claim must fail", WeldCoreUtils.claimProxyCallTrace(sig1));
        assertFalse("sig2 second claim must fail", WeldCoreUtils.claimProxyCallTrace(sig2));
    }

    /** Releasing a signature that was never claimed does not throw. */
    @Test
    public void testRelease_NeverClaimedSignature_NoException() {
        // Should be a no-op, not an exception
        WeldCoreUtils.releaseProxyCallTrace("test.DedupClass:neverClaimed");
    }

    /**
     * ThreadLocal isolation: the main thread's claim does NOT block a second thread
     * from claiming the same signature (each thread has its own set).
     */
    @Test
    public void testThreadIsolation_IndependentPerThread() throws InterruptedException {
        String sig = "test.DedupClass:threadIsolated";
        claimedSignatures.add(sig);

        // Claim on main thread
        assertTrue("Main thread claim must succeed", WeldCoreUtils.claimProxyCallTrace(sig));

        // Child thread should be able to claim the same signature (its own ThreadLocal)
        final boolean[] childResult = {false};
        Thread child = new Thread(() -> {
            childResult[0] = WeldCoreUtils.claimProxyCallTrace(sig);
            WeldCoreUtils.releaseProxyCallTrace(sig);  // clean up child's ThreadLocal
        });
        child.start();
        child.join(2000L);

        assertTrue("Child thread must claim independently (ThreadLocal isolation)",
            childResult[0]);
    }

    /**
     * Simulates a Weld interceptor chain re-entry scenario:
     * outer proceed() claims → inner proceed() is blocked → outer finally releases.
     * This is the core dedup behaviour that prevents N nested identical spans.
     */
    @Test
    public void testInterceptorChainReentry_Simulation() {
        String sig = "com.example.MyService:performAction";

        // Outer proceed() claims — creates the single tracer
        boolean outerClaimed = WeldCoreUtils.claimProxyCallTrace(sig);
        claimedSignatures.add(sig);

        // Inner proceed() (re-entry from second interceptor) — must be blocked
        boolean innerClaimed = WeldCoreUtils.claimProxyCallTrace(sig);

        // Outer finally releases
        WeldCoreUtils.releaseProxyCallTrace(sig);
        claimedSignatures.remove(sig);  // already released

        // After outer releases, another outer call on same thread may proceed
        boolean afterReleaseClaimed = WeldCoreUtils.claimProxyCallTrace(sig);
        claimedSignatures.add(sig);

        assertTrue("Outer proceed() should claim successfully", outerClaimed);
        assertFalse("Inner proceed() (re-entry) must be blocked", innerClaimed);
        assertTrue("After release, a new invocation must claim successfully", afterReleaseClaimed);
    }
}
