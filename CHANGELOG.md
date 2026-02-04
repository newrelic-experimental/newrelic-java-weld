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
