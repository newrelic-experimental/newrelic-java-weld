package com.newrelic.instrumentation.labs.weld.config;

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.Logger;
import com.newrelic.api.agent.NewRelic;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.logging.Level;

/**
 * Configuration class for Weld instrumentation filtering using whitelist approach.
 * Provides separate filtering for BeanInstance and ProxyCall components.
 *
 * Configuration keys:
 * - weld.filtering_enabled: boolean (default: false)
 * - weld.beaninstance.track_full_names: List of exact matches (e.g., "com.example.Service:login")
 * - weld.beaninstance.track_regex_patterns: List of regex patterns
 * - weld.proxycall.track_full_names: List of exact matches
 * - weld.proxycall.track_regex_patterns: List of regex patterns
 *
 * When filtering_enabled=false: All methods are traced (backward compatible)
 * When filtering_enabled=true: Only methods matching whitelist are traced
 */
public class WeldTraceFilterConfig implements AgentConfigListener {

    private static final String CONFIG_NAMESPACE = "weld";
    private static final String FILTERING_ENABLED_KEY = CONFIG_NAMESPACE + ".filtering_enabled";

    // BeanInstance configuration keys
    private static final String BEAN_FULL_NAMES_KEY = CONFIG_NAMESPACE + ".beaninstance.track_full_names";
    private static final String BEAN_REGEX_PATTERNS_KEY = CONFIG_NAMESPACE + ".beaninstance.track_regex_patterns";

    // ProxyCall configuration keys
    private static final String PROXY_FULL_NAMES_KEY = CONFIG_NAMESPACE + ".proxycall.track_full_names";
    private static final String PROXY_REGEX_PATTERNS_KEY = CONFIG_NAMESPACE + ".proxycall.track_regex_patterns";

    // State - BeanInstance
    private static volatile boolean filteringEnabled = false;
    private static volatile Set<String> beanFullNames = new CopyOnWriteArraySet<>();
    private static volatile Set<Pattern> beanRegexPatterns = new CopyOnWriteArraySet<>();

    // State - ProxyCall
    private static volatile Set<String> proxyFullNames = new CopyOnWriteArraySet<>();
    private static volatile Set<Pattern> proxyRegexPatterns = new CopyOnWriteArraySet<>();

    private static volatile Logger logger = null;

    static {
        try {
            logger = NewRelic.getAgent().getLogger();
            WeldTraceFilterConfig listener = new WeldTraceFilterConfig();
            ServiceFactory.getConfigService().addIAgentConfigListener(listener);
            listener.loadConfig(NewRelic.getAgent().getConfig());
        } catch (Exception e) {
            // Initialization failed - likely in test environment
            // Logger will be null, and logging will be skipped
        }
    }

    /**
     * Get logger with null safety for testing
     */
    private static Logger getLogger() {
        return logger;
    }

    /**
     * Log with null safety
     */
    private static void log(Level level, String message, Object... params) {
        Logger l = getLogger();
        if (l != null) {
            l.log(level, message, params);
        }
    }

    /**
     * Log exception with null safety
     */
    private static void log(Level level, Throwable throwable, String message, Object... params) {
        Logger l = getLogger();
        if (l != null) {
            l.log(level, throwable, message, params);
        }
    }

    // Package-private constructor for testing
    WeldTraceFilterConfig() {
        // Constructor accessible to tests
    }

    @Override
    public void configChanged(String appName, AgentConfig newConfig) {
        log(Level.INFO, "New Relic WeldTraceFilterConfig: Configuration changed for app {0}", appName);
        loadConfig(newConfig);
    }

    private void loadConfig(Config config) {
        // Load filtering enabled flag
        Boolean enabled = config.getValue(FILTERING_ENABLED_KEY);
        if (enabled != null) {
            filteringEnabled = enabled;
            log(Level.INFO, "New Relic WeldTraceFilterConfig: Filtering enabled: {0}", filteringEnabled);
        } else {
            filteringEnabled = false;
            log(Level.INFO, "New Relic WeldTraceFilterConfig: Filtering disabled (no config found for {0})", FILTERING_ENABLED_KEY);
        }

        // Load BeanInstance configuration
        loadBeanInstanceConfig(config);

        // Load ProxyCall configuration
        loadProxyCallConfig(config);
    }

    private void loadBeanInstanceConfig(Config config) {
        // Load BeanInstance full names
        List<String> beanFullNamesList = config.getValue(BEAN_FULL_NAMES_KEY);
        if (beanFullNamesList != null && !beanFullNamesList.isEmpty()) {
            beanFullNames = new CopyOnWriteArraySet<>(beanFullNamesList);
            log(Level.INFO, "New Relic WeldTraceFilterConfig: Loaded {0} BeanInstance full names", beanFullNames.size());
            for (String name : beanFullNames) {
                log(Level.FINE, "New Relic WeldTraceFilterConfig: BeanInstance track full name: {0}", name);
            }
        } else {
            beanFullNames = Collections.emptySet();
            log(Level.INFO, "New Relic WeldTraceFilterConfig: No BeanInstance full names configured");
        }

        // Load BeanInstance regex patterns
        List<String> beanPatternsList = config.getValue(BEAN_REGEX_PATTERNS_KEY);
        if (beanPatternsList != null && !beanPatternsList.isEmpty()) {
            Set<Pattern> newPatterns = new CopyOnWriteArraySet<>();
            for (String patternString : beanPatternsList) {
                try {
                    Pattern pattern = Pattern.compile(patternString);
                    newPatterns.add(pattern);
                    log(Level.FINE, "New Relic WeldTraceFilterConfig: Added BeanInstance pattern: {0}", patternString);
                } catch (PatternSyntaxException e) {
                    log(Level.WARNING, e, "New Relic WeldTraceFilterConfig: Invalid BeanInstance regex pattern: {0}", patternString);
                }
            }
            beanRegexPatterns = newPatterns;
            log(Level.INFO, "New Relic WeldTraceFilterConfig: Loaded {0} BeanInstance regex patterns", beanRegexPatterns.size());
        } else {
            beanRegexPatterns = Collections.emptySet();
            log(Level.INFO, "New Relic WeldTraceFilterConfig: No BeanInstance regex patterns configured");
        }
    }

    private void loadProxyCallConfig(Config config) {
        // Load ProxyCall full names
        List<String> proxyFullNamesList = config.getValue(PROXY_FULL_NAMES_KEY);
        if (proxyFullNamesList != null && !proxyFullNamesList.isEmpty()) {
            proxyFullNames = new CopyOnWriteArraySet<>(proxyFullNamesList);
            log(Level.INFO, "New Relic WeldTraceFilterConfig: Loaded {0} ProxyCall full names", proxyFullNames.size());
            for (String name : proxyFullNames) {
                log(Level.FINE, "New Relic WeldTraceFilterConfig: ProxyCall track full name: {0}", name);
            }
        } else {
            proxyFullNames = Collections.emptySet();
            log(Level.INFO, "New Relic WeldTraceFilterConfig: No ProxyCall full names configured");
        }

        // Load ProxyCall regex patterns
        List<String> proxyPatternsList = config.getValue(PROXY_REGEX_PATTERNS_KEY);
        if (proxyPatternsList != null && !proxyPatternsList.isEmpty()) {
            Set<Pattern> newPatterns = new CopyOnWriteArraySet<>();
            for (String patternString : proxyPatternsList) {
                try {
                    Pattern pattern = Pattern.compile(patternString);
                    newPatterns.add(pattern);
                    log(Level.FINE, "New Relic WeldTraceFilterConfig: Added ProxyCall pattern: {0}", patternString);
                } catch (PatternSyntaxException e) {
                    log(Level.WARNING, e, "New Relic WeldTraceFilterConfig: Invalid ProxyCall regex pattern: {0}", patternString);
                }
            }
            proxyRegexPatterns = newPatterns;
            log(Level.INFO, "New Relic WeldTraceFilterConfig: Loaded {0} ProxyCall regex patterns", proxyRegexPatterns.size());
        } else {
            proxyRegexPatterns = Collections.emptySet();
            log(Level.INFO, "New Relic WeldTraceFilterConfig: No ProxyCall regex patterns configured");
        }
    }

    /**
     * Determines if a BeanInstance method should be traced based on whitelist configuration.
     *
     * @param className Fully qualified class name (e.g., "com.example.service.UserService")
     * @param methodName Method name (e.g., "login")
     * @return true if the method should be traced, false otherwise
     */
    public static boolean shouldTraceBeanInstance(String className, String methodName) {
        // If filtering is disabled, trace everything (backward compatible)
        if (!filteringEnabled) {
            return true;
        }

        // Build fully qualified method name
        String fullyQualifiedMethodName = className + ":" + methodName;

        // Check full name match (exact match)
        if (beanFullNames.contains(fullyQualifiedMethodName)) {
            log(Level.FINEST, "New Relic WeldTraceFilterConfig: BeanInstance matched full name: {0}", fullyQualifiedMethodName);
            return true;
        }

        // Check regex pattern match
        for (Pattern pattern : beanRegexPatterns) {
            if (pattern.matcher(fullyQualifiedMethodName).matches()) {
                log(Level.FINEST, "New Relic WeldTraceFilterConfig: BeanInstance matched pattern {0}: {1}",
                    pattern.pattern(), fullyQualifiedMethodName);
                return true;
            }
        }

        // No match - don't trace
        log(Level.FINEST, "New Relic WeldTraceFilterConfig: BeanInstance not whitelisted: {0}", fullyQualifiedMethodName);
        return false;
    }

    /**
     * Determines if a ProxyCall method should be traced based on whitelist configuration.
     *
     * @param className Fully qualified class name (e.g., "com.example.service.UserService")
     * @param methodName Method name (e.g., "login")
     * @return true if the method should be traced, false otherwise
     */
    public static boolean shouldTraceProxyCall(String className, String methodName) {
        // If filtering is disabled, trace everything (backward compatible)
        if (!filteringEnabled) {
            return true;
        }

        // Build fully qualified method name
        String fullyQualifiedMethodName = className + ":" + methodName;

        // Check full name match (exact match)
        if (proxyFullNames.contains(fullyQualifiedMethodName)) {
            log(Level.FINEST, "New Relic WeldTraceFilterConfig: ProxyCall matched full name: {0}", fullyQualifiedMethodName);
            return true;
        }

        // Check regex pattern match
        for (Pattern pattern : proxyRegexPatterns) {
            if (pattern.matcher(fullyQualifiedMethodName).matches()) {
                log(Level.FINEST, "New Relic WeldTraceFilterConfig: ProxyCall matched pattern {0}: {1}",
                    pattern.pattern(), fullyQualifiedMethodName);
                return true;
            }
        }

        // No match - don't trace
        log(Level.FINEST, "New Relic WeldTraceFilterConfig: ProxyCall not whitelisted: {0}", fullyQualifiedMethodName);
        return false;
    }

    // Package-private methods for testing
    static boolean isFilteringEnabled() {
        return filteringEnabled;
    }

    static Set<String> getBeanFullNames() {
        return Collections.unmodifiableSet(beanFullNames);
    }

    static Set<Pattern> getBeanRegexPatterns() {
        return Collections.unmodifiableSet(beanRegexPatterns);
    }

    static Set<String> getProxyFullNames() {
        return Collections.unmodifiableSet(proxyFullNames);
    }

    static Set<Pattern> getProxyRegexPatterns() {
        return Collections.unmodifiableSet(proxyRegexPatterns);
    }
}
