# Weld CDI Instrumentation - Configuration Guide

## Table of Contents
- [Overview](#overview)
- [Quick Start](#quick-start)
- [Configuration Options](#configuration-options)
- [Configuration Examples](#configuration-examples)
- [Filter Priority](#filter-priority)
- [Performance Considerations](#performance-considerations)
- [Troubleshooting](#troubleshooting)

## Overview

This instrumentation provides visibility into JBoss Weld CDI (Contexts and Dependency Injection) proxy method invocations with configurable filtering to control which methods are traced.

### Supported Weld Versions
- Weld Core 3.x
- Weld Core 4.x (including Weld 6.0.2+)

### Metric Types
- **Custom/Weld/ProxyCall**: Traces from CDI interceptor chains (requires transaction context)
- **Custom/Weld/ContextBean**: Traces from Weld context bean invocations (standalone apps)
- **Custom/Weld/BeanInstance**: Traces from bean instance invocations (web applications)

## Quick Start

### Default Behavior (No Configuration)
By default, all CDI proxy methods are traced. No configuration needed:

```yaml
# newrelic.yml - default behavior
common: &default_settings
  app_name: my-weld-app
  # Weld configuration optional - defaults to trace everything
```

### Enable Whitelist Filtering
Trace only specific methods to reduce overhead:

```yaml
common: &default_settings
  app_name: my-weld-app

  weld:
    filtering_enabled: true
    proxycall:
      track_full_names:
        - "com.example.MyService:businessMethod"
        - "com.example.OrderService:processOrder"
```

## Configuration Options

### Core Settings

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| `weld.filtering_enabled` | boolean | `false` | Enable whitelist filtering. When `true`, ONLY methods in whitelist are traced |
| `weld.ignore_traces_enabled` | boolean | `false` | Enable blacklist filtering. When `true`, methods matching ignore patterns are NOT traced |

### Whitelist Options (ProxyCall)

| Setting | Type | Description |
|---------|------|-------------|
| `weld.proxycall.track_full_names` | list | Exact class:method names to trace (e.g., `"com.example.MyClass:myMethod"`) |
| `weld.proxycall.track_regex_patterns` | list | Regex patterns for matching methods (e.g., `"com\\.example\\..*Service:process.*"`) |

### Whitelist Options (BeanInstance)

| Setting | Type | Description |
|---------|------|-------------|
| `weld.beaninstance.track_full_names` | list | Exact class:method names for BeanInstance tracing |
| `weld.beaninstance.track_regex_patterns` | list | Regex patterns for BeanInstance tracing |

### Blacklist Options

| Setting | Type | Description |
|---------|------|-------------|
| `weld.ignored_trace_patterns` | list | Wildcard patterns for methods to ignore (e.g., `"com.example.MyClass:*"`, `"*.Internal*:*"`) |

## Configuration Examples

### Example 1: Whitelist - Exact Match
Trace only explicitly listed methods:

```yaml
weld:
  filtering_enabled: true

  proxycall:
    track_full_names:
      - "com.example.OrderService:createOrder"
      - "com.example.OrderService:updateOrder"
      - "com.example.PaymentService:processPayment"

  beaninstance:
    track_full_names: []  # Don't trace BeanInstance
```

**Result:**
- ✅ Traces: `OrderService:createOrder`, `OrderService:updateOrder`, `PaymentService:processPayment`
- ❌ Ignores: All other methods

### Example 2: Whitelist - Regex Patterns
Trace methods matching patterns:

```yaml
weld:
  filtering_enabled: true

  proxycall:
    track_regex_patterns:
      - "com\\.example\\..*Service:process.*"  # All *Service classes, process* methods
      - "com\\.example\\.core\\..*:execute.*"  # All core.* classes, execute* methods
```

**Result:**
- ✅ Traces: `OrderService:processOrder`, `PaymentService:processPayment`, `core.TaskExecutor:executeTask`
- ❌ Ignores: `OrderService:createOrder`, `DataRepository:findAll`

### Example 3: Whitelist - Combined (Exact + Regex)
Use both exact names and patterns:

```yaml
weld:
  filtering_enabled: true

  proxycall:
    track_full_names:
      - "com.example.CriticalService:importantMethod"  # Must trace this
    track_regex_patterns:
      - "com\\.example\\..*Service:process.*"  # Plus all process* methods
```

**Result:**
- ✅ Traces: `CriticalService:importantMethod` (exact), `OrderService:processPayment` (regex)
- ❌ Ignores: Everything else

### Example 4: Blacklist Only
Trace everything EXCEPT specific methods:

```yaml
weld:
  filtering_enabled: false  # Don't use whitelist

  ignore_traces_enabled: true
  ignored_trace_patterns:
    - "com.example.HealthCheck:*"  # Ignore all HealthCheck methods
    - "com.example.Metrics:*"  # Ignore all Metrics methods
    - "*.Internal*:*"  # Ignore all classes containing "Internal"
```

**Result:**
- ✅ Traces: All methods except HealthCheck, Metrics, and Internal classes
- ❌ Ignores: `HealthCheck:ping`, `Metrics:collect`, `InternalService:process`

### Example 5: Whitelist + Blacklist (Combined)
Whitelist specific methods, then blacklist a subset:

```yaml
weld:
  filtering_enabled: true

  proxycall:
    track_regex_patterns:
      - "com\\.example\\..*Service:.*"  # Whitelist all *Service methods

  ignore_traces_enabled: true
  ignored_trace_patterns:
    - "com.example.HealthCheckService:*"  # But ignore HealthCheckService
    - "*.MetricsService:collect*"  # And ignore collect* methods
```

**Result:**
- ✅ Traces: `OrderService:createOrder`, `PaymentService:processPayment`
- ❌ Ignores: `HealthCheckService:ping` (blacklisted), `MetricsService:collectStats` (blacklisted)
- **Note**: Blacklist takes precedence over whitelist

### Example 6: Empty Whitelist
Disable all tracing with empty whitelist:

```yaml
weld:
  filtering_enabled: true

  proxycall:
    track_full_names: []
    track_regex_patterns: []
```

**Result:**
- ❌ No methods traced (empty whitelist blocks everything)

### Example 7: Production Optimized
Minimal overhead for production:

```yaml
weld:
  filtering_enabled: true

  proxycall:
    track_regex_patterns:
      - "com\\.mycompany\\.business\\..*:execute.*"  # Only business logic
      - "com\\.mycompany\\.api\\..*Controller:.*"  # And API controllers

  ignore_traces_enabled: true
  ignored_trace_patterns:
    - "*.health.*:*"  # Skip health checks
    - "*.metrics.*:*"  # Skip metrics
    - "*.logging.*:*"  # Skip logging utilities

  beaninstance:
    track_regex_patterns: []  # Disable BeanInstance tracing
```

### Example 8: Development/Debugging
Verbose tracing for development:

```yaml
weld:
  filtering_enabled: false  # Trace everything
  ignore_traces_enabled: false  # Don't ignore anything

log_level: finest  # See all filter decisions in agent log
```

## Filter Priority

Filters are applied in this order:

1. **Blacklist (ignore patterns)** - Checked first, takes highest precedence
2. **Whitelist (track patterns)** - Checked second, only if not blacklisted
3. **Default** - If filtering disabled, all methods traced

### Decision Flow

```
Method invoked
    ↓
Is blacklisted? (ignore patterns)
    ↓ YES → Skip tracing (no metric created)
    ↓ NO
    ↓
Is filtering enabled?
    ↓ NO → Create trace (default behavior)
    ↓ YES
    ↓
Is whitelisted? (exact name OR regex pattern)
    ↓ YES → Create trace
    ↓ NO → Skip tracing (no metric created)
```

### Example: Combined Filtering

```yaml
weld:
  filtering_enabled: true
  proxycall:
    track_regex_patterns:
      - "com\\.example\\..*:.*"  # Whitelist all com.example.*

  ignore_traces_enabled: true
  ignored_trace_patterns:
    - "com.example.Test*:*"  # Blacklist Test* classes
```

**Results:**
- `com.example.OrderService:create` → ✅ Traced (whitelisted, not blacklisted)
- `com.example.TestHelper:setup` → ❌ Ignored (whitelisted BUT blacklisted - blacklist wins)
- `com.other.Service:method` → ❌ Ignored (not whitelisted)

## Performance Considerations

### Memory Impact

**Filtered methods (not traced):**
- No tracer objects created
- No metric data collected
- No span events generated
- Minimal CPU for filter check only

**Traced methods:**
- Creates tracer object (~1-2 KB memory)
- Collects timing and metric data
- Generates span events (if enabled)

### Best Practices

1. **Use Whitelist in Production**
   ```yaml
   filtering_enabled: true
   proxycall:
     track_regex_patterns:
       - "com\\.mycompany\\.critical\\..*:.*"  # Only business-critical classes
   ```

2. **Blacklist High-Frequency Methods**
   ```yaml
   ignore_traces_enabled: true
   ignored_trace_patterns:
     - "*.HealthCheck:*"  # Called every 10s
     - "*.Heartbeat:*"    # Called every 5s
   ```

3. **Avoid Over-Broad Patterns**
   ```yaml
   # ❌ BAD: Matches everything
   track_regex_patterns:
     - ".*:.*"

   # ✅ GOOD: Matches specific packages
   track_regex_patterns:
     - "com\\.mycompany\\.business\\..*:.*"
   ```

4. **Combine Strategies for Optimal Performance**
   ```yaml
   # Whitelist business logic + blacklist high-frequency
   filtering_enabled: true
   proxycall:
     track_regex_patterns:
       - "com\\.mycompany\\.business\\..*:.*"

   ignore_traces_enabled: true
   ignored_trace_patterns:
     - "*.Cache*:*"
     - "*.Pool*:*"
   ```

## Dynamic Configuration

Configuration changes in `newrelic.yml` are detected automatically via `AgentConfigListener`:

1. Edit `newrelic.yml` with new filter patterns
2. Changes take effect within 60 seconds (next config poll)
3. No agent restart required
4. Check agent log for confirmation:
   ```
   INFO: New Relic WeldTraceFilterConfig: Filtering enabled: true
   INFO: New Relic WeldTraceFilterConfig: Loaded 3 ProxyCall regex patterns
   ```

## Pattern Syntax

### Exact Names
Format: `"fully.qualified.ClassName:methodName"`

```yaml
track_full_names:
  - "com.example.OrderService:createOrder"
  - "com.example.PaymentService:processPayment"
```

### Regex Patterns
Format: Java regex with escaped dots

```yaml
track_regex_patterns:
  - "com\\.example\\..*Service:.*"  # All *Service classes, all methods
  - "com\\.example\\.core\\..*:execute.*"  # core.* package, execute* methods
  - ".*\\.business\\..*:process.*"  # Any business package, process* methods
```

**Important**: Escape dots in package names as `\\.` (YAML requires double backslash)

### Wildcard Patterns (Blacklist Only)
Format: Simple wildcards with `*`

```yaml
ignored_trace_patterns:
  - "com.example.TestHelper:*"  # All methods on TestHelper
  - "*.Internal*:*"  # All classes containing "Internal"
  - "com.example.*:toString"  # All toString methods in com.example
```

## Troubleshooting

### Issue: Whitelist not working (all methods still traced)

**Check:**
```yaml
weld:
  filtering_enabled: true  # ← Must be true!
  proxycall:
    track_full_names:
      - "com.example.MyService:myMethod"
```

**Verify in agent log:**
```bash
grep "WeldTraceFilterConfig: Filtering enabled" newrelic/logs/*.log
# Should show: "Filtering enabled: true"
```

### Issue: Blacklist patterns not matching

**Check wildcard syntax:**
```yaml
# ✅ CORRECT:
ignored_trace_patterns:
  - "com.example.MyClass:*"  # Simple wildcard

# ❌ WRONG:
ignored_trace_patterns:
  - "com.example.MyClass:.*"  # This is regex syntax, won't work
```

**Verify pattern loading:**
```bash
grep "TraceIgnoreConfig: Added ignore pattern" newrelic/logs/*.log
# Check the converted regex pattern
```

### Issue: Metrics still showing proxy suffixes

**Example**: `Custom/Weld/ProxyCall/com.example.MyService$Proxy$_$$_WeldSubclass/method`

**Solution**: Upgrade to v2.0.0+ which automatically cleans proxy class names.

**Expected**: `Custom/Weld/ProxyCall/com.example.MyService/method`

### Issue: No metrics appearing at all

**Check log level** (set to `fine` or `finest` for debugging):
```yaml
log_level: fine
```

**Check agent log for filtering decisions:**
```bash
# See which methods are whitelisted/blacklisted
grep -E "(whitelisted|blacklisted|matched pattern)" newrelic/logs/*.log

# See filter configuration loading
grep -E "(WeldTraceFilterConfig|TraceIgnoreConfig)" newrelic/logs/*.log
```

### Issue: High memory usage

**Solution**: Enable filtering to reduce traced methods:

```yaml
weld:
  filtering_enabled: true
  proxycall:
    track_regex_patterns:
      - "com\\.mycompany\\.critical\\..*:.*"  # Only critical packages

  ignore_traces_enabled: true
  ignored_trace_patterns:
    - "*.Cache*:*"  # High-frequency methods
    - "*.Pool*:*"
    - "*.health.*:*"
```

## Advanced Configuration

### Regex Pattern Tips

**Match all methods in a package:**
```yaml
track_regex_patterns:
  - "com\\.example\\.services\\..*:.*"
```

**Match specific method patterns:**
```yaml
track_regex_patterns:
  - ".*:execute.*"  # All execute* methods
  - ".*:process.*"  # All process* methods
  - ".*Service:handle.*"  # All *Service classes, handle* methods
```

**Match nested packages:**
```yaml
track_regex_patterns:
  - "com\\.example\\.(api|business)\\..*:.*"  # Match api OR business packages
```

### Wildcard Pattern Tips

**Match class hierarchies:**
```yaml
ignored_trace_patterns:
  - "com.example.base.*:*"  # All classes in base package and subpackages
```

**Match method name patterns:**
```yaml
ignored_trace_patterns:
  - "*:get*"  # All getter methods (not recommended - too broad)
  - "*:toString"  # All toString methods
  - "*:hashCode"  # All hashCode methods
```

### Testing Your Configuration

1. **Enable verbose logging:**
   ```yaml
   log_level: finest
   ```

2. **Test a method invocation**

3. **Check agent log:**
   ```bash
   # See filter decisions
   grep "WeldTraceFilterConfig" newrelic/logs/*.log | tail -50

   # See what patterns matched
   grep "matched pattern" newrelic/logs/*.log

   # See what was blacklisted
   grep "Ignoring trace" newrelic/logs/*.log
   ```

4. **Verify metrics created:**
   ```bash
   # In New Relic, run NRQL:
   SELECT count(*) FROM Metric
   WHERE appName = 'your-app-name'
     AND metricName LIKE 'Custom/Weld/%'
   FACET metricName
   ```

## Migration from v1.0.0

### No Breaking Changes
All v1.0.0 configurations continue to work unchanged. New filtering features are opt-in.

### Recommended Upgrades

**If experiencing high memory usage:**
```yaml
# Add whitelist to reduce traced methods
weld:
  filtering_enabled: true
  proxycall:
    track_regex_patterns:
      - "com\\.yourcompany\\.business\\..*:.*"
```

**If seeing proxy suffixes in metric names:**
- Upgrade to v2.0.0+ (automatic cleaning)
- No configuration change needed

**If using custom ignore patterns:**
- Verify wildcard patterns work correctly:
  ```yaml
  # v2.0.0+ correctly handles wildcards
  ignored_trace_patterns:
    - "com.example.MyClass:*"  # Now works correctly
  ```

## Support

For issues or questions:
- GitHub Issues: https://github.com/newrelic-experimental/newrelic-java-weld/issues
- New Relic Docs: https://docs.newrelic.com/

## Version History

See [CHANGELOG.md](CHANGELOG.md) for detailed version history.
