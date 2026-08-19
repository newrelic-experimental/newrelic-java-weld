## Version: v2.5 | Created: 2026-08-19

### Performance Fix — Root Cause of Test 11/12 Residual Span Overhead

`CombinedInterceptorAndDecoratorStackMethodHandler_Instrumentation`, `InterceptorMethodHandler_Instrumentation`, and `StackAwareMethodHandler_Instrumentation` (weld-core-3.0) previously used unconditional `@Trace` annotations on their `invoke()` methods.

**Problem**: A `@Trace` span is created by the NR agent BEFORE any Java code in the annotated method executes. This means the whitelist/blacklist config was never consulted — spans were created for every CDI call including high-frequency security framework classes (`MasterTenantService`, `SecurityHelper`, etc.) that the whitelist was supposed to suppress. This was confirmed by T11/T12 trace analysis:
- ProxyCall spans for `security.internal.security.*` = **0** ✓ (whitelist working via ExitTracer)
- StackAwareMethodHandler + InterceptorMethodHandler spans for same classes = **960** ✗ (not filtered, `@Trace`)
- Total residual security noise = **1,640 / 3,002 spans (54%)** — unchanged between T11 and T12

**Fix** (both modules): Replaced `@Trace` with conditional `ExitTracer` (same pattern as `BeanInstance_Instrumentation.invoke()`). ExitTracer is only created when `shouldTraceProxyCall() && !shouldIgnoreTrace()` pass. Non-whitelisted classes produce **zero spans** from these weave points.

**Expected result for T13**:
- security.internal.security.* StackAwareMethodHandler spans: 0 (was ~480)
- security.internal.security.* InterceptorMethodHandler spans: 0 (was ~480)
- Java/CombinedInterceptorAndDecoratorStackMethodHandler spans for filtered classes: 0 (was ~480)
- EJB spans for security.* (NR built-in): unchanged (~680) — requires `class_transformer.excludes`
- Estimated net span count: **~1,600 (47% below T12 baseline of 3,002)**

### Files Changed
- `weld-core-4.0/.../CombinedInterceptorAndDecoratorStackMethodHandler_Instrumentation.java`
- `weld-core-4.0/.../InterceptorMethodHandler_Instrumentation.java`
- `weld-core-3.0/.../StackAwareMethodHandler_Instrumentation.java`
- `weld-core-3.0/.../CombinedInterceptorAndDecoratorStackMethodHandler_Instrumentation.java`
- `weld-core-3.0/.../InterceptorMethodHandler_Instrumentation.java`

### Build
- All three modules bumped to `Implementation-Version: 2.5` (single decimal visible in NR UI)

### Breaking Changes
- None — behavior for whitelisted methods is unchanged. Only non-whitelisted methods lose StackAwareMethodHandler/InterceptorMethodHandler spans.

---

## Version: v2.0.3 | Created: 2026-08-12

### Performance Fix
- **Filter order corrected in `WeldCoreUtils.addMethod()`**: Whitelist check now runs before blacklist in both `weld-core-3.0` and `weld-core-4.0`. Non-whitelisted methods exit immediately without paying the regex cost of the blacklist check. Previously the blacklist ran first for every CDI method at class-instrumentation time (startup), though the hot-path methods `BeanInstance_Instrumentation.invoke()` and `WeldCoreUtils.createProxyCallTracer()` were already correct from v2.1.0.

### Build
- All three modules (`weld-core-3.0`, `weld-core-4.0`, `weld-ejb`) bumped to `Implementation-Version: 2.0.3` (quoted string to avoid Gradle float coercion).

### Breaking Changes
- None — backward compatible.

---

## Version: v2.1.0 | Created: 2026-08-10

### Bug Fixes
- **BeanInstance blacklist**: Added `TraceIgnoreConfig` blacklist check to `BeanInstance_Instrumentation.invoke()`. `Custom/Weld/BeanInstance/ContextBeanInstance/invoke/` spans (e.g. `ExecutableValidator`, `BeanMetaDataClassNormalizer`) were not suppressed by `ignore_traces_enabled` patterns because the blacklist check was missing from this code path.
- **Duplicate ProxyCall spans**: Weld calls `AroundInvokeInvocationContext.proceed()` once per interceptor for the same method, producing N nested `Custom/Weld/ProxyCall` spans. Added per-thread deduplication so only the outermost `proceed()` call per method creates a tracer.

### New Classes
- `NonTerminalAroundInvokeInvocationContext_Instrumentation` (both modules): Registers the Weld 6.x concrete subclass in the weave hierarchy for correct instrumentation via inheritance.

### Build
- Updated `java.agent.version` to 9.4.0

### Breaking Changes
- None — backward compatible. Default behavior unchanged.

---

## Version: [v2.0.0](https://github.com/newrelic-experimental/newrelic-java-weld/releases/tag/v2.0.0) | Created: 2026-02-04
### Features
- **Whitelist Filtering**: Selectively trace only specific CDI proxy methods using exact names or regex patterns
- **Blacklist Filtering**: Ignore specific CDI proxy methods using wildcard patterns (e.g., `MyClass:*`)
- **Dynamic Configuration**: Filter settings read dynamically from `newrelic.yml` without restart via `AgentConfigListener`
- **Proxy Class Name Normalization**: Remove Weld proxy suffixes (e.g., `$Proxy$_$$_WeldSubclass`) from all metric names
- **Memory Optimization**: Filtered methods don't create tracers, reducing memory overhead

### Bug Fixes
- Fixed wildcard pattern matching in `TraceIgnoreConfig.convertWildcardToRegex()` - wildcards now correctly expand to regex `.*`
- Removed incorrect `Transaction.ignore()` call from `AroundInvokeInvocationContext` - filtered methods no longer affect transaction reporting
- Applied consistent proxy class name cleaning across all instrumentation points (AroundInvokeInvocationContext, BeanInstance, ContextBeanInstance)

### Breaking Changes
- None - all enhancements are backward compatible. Default behavior (no filtering) unchanged.

---

## Version: [v1.0.0](https://github.com/newrelic-experimental/newrelic-java-weld/releases/tag/v1.0.0) | Created: 2025-07-16
### Build Upgrades
- First baseline build

### Features
- Dynamically ignore and clean New Relic traces for Weld proxies
- Merge pull request #2 from newrelic-experimental/feat-metric-naming-1
- Merge pull request #3 from newrelic-experimental/feat-metric-naming

### Bug Fixes
- Merge pull request #5 from newrelic-experimental/fix_class_name_cleanup

## Installation

To install:

1. Download the latest release jar files.
2. In the New Relic Java directory (the one containing newrelic.jar), create a directory named extensions if it does not already exist.
3. Copy the downloaded jars into the extensions directory.
4. Restart the application.   
