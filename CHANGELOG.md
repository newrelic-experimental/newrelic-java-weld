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
