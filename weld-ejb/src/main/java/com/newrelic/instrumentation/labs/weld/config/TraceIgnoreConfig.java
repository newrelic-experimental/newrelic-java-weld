// src/main/java/com/newrelic/instrumentation/labs/weld/config/TraceIgnoreConfig.java
package com.newrelic.instrumentation.labs.weld.config; // New package for config

import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.AgentConfigListener;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.api.agent.Config;
import com.newrelic.api.agent.NewRelic;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.logging.Level;
import com.newrelic.api.agent.Logger;

public class TraceIgnoreConfig implements AgentConfigListener {

    private static final String CONFIG_NAMESPACE = "weld";
    private static final String ENABLED_KEY = CONFIG_NAMESPACE + ".ignore_traces_enabled";
    private static final String PATTERNS_KEY = CONFIG_NAMESPACE + ".ignored_trace_patterns";

    private static volatile boolean ignoreTracesEnabled = false;
    private static volatile Set<Pattern> ignorePatterns = new CopyOnWriteArraySet<>();

    // shouldIgnoreTrace runs on every whitelisted CDI call — caching per method name avoids
    // re-running the pattern loop on every call. Cleared on config reload.
    private static final ConcurrentHashMap<String, Boolean> decisionCache = new ConcurrentHashMap<>();

    // Nested cache for the (className, methodName) overload — lets callers that already hold
    // the two Strings separately skip building a throwaway "class:method" key on every call.
    private static final ConcurrentHashMap<String, ConcurrentHashMap<String, Boolean>> decisionCacheByClass = new ConcurrentHashMap<>();

    private static final Logger logger = NewRelic.getAgent().getLogger();

    static {
        TraceIgnoreConfig listener = new TraceIgnoreConfig();
        ServiceFactory.getConfigService().addIAgentConfigListener(listener);
        listener.loadConfig(NewRelic.getAgent().getConfig());
    }

    private TraceIgnoreConfig() {
        // Private constructor for singleton pattern
    }

    @Override
    public void configChanged(String appName, AgentConfig newConfig) {
        logger.log(Level.INFO, "New Relic TraceIgnoreConfig: Configuration changed for app {0}", appName);
        loadConfig(newConfig);
    }

    private void loadConfig(Config config) {
        // Decisions cached under the old config are no longer valid once patterns change.
        decisionCache.clear();
        decisionCacheByClass.clear();

        Boolean enabled = config.getValue(ENABLED_KEY);
        if (enabled != null) {
            ignoreTracesEnabled = enabled;
            logger.log(Level.INFO, "New Relic TraceIgnoreConfig: Trace ignoring enabled: {0}", ignoreTracesEnabled);
        } else {
            ignoreTracesEnabled = false;
            logger.log(Level.INFO, "New Relic TraceIgnoreConfig: Trace ignoring disabled (no config found for {0}).", ENABLED_KEY);
        }

        List<String> patterns = config.getValue(PATTERNS_KEY);
        if (patterns != null && !patterns.isEmpty()) {
            Set<Pattern> newPatterns = new CopyOnWriteArraySet<>();
            for (String patternString : patterns) {
                try {
                    String regexPattern = convertWildcardToRegex(patternString);
                    newPatterns.add(Pattern.compile(regexPattern));
                    logger.log(Level.FINE, "New Relic TraceIgnoreConfig: Added ignore pattern: {0} (regex: {1})", patternString, regexPattern);
                } catch (PatternSyntaxException e) {
                    logger.log(Level.WARNING, e, "New Relic TraceIgnoreConfig: Invalid regex pattern in config: {0}", patternString);
                }
            }
            ignorePatterns = newPatterns;
            logger.log(Level.INFO, "New Relic TraceIgnoreConfig: Loaded {0} ignore patterns.", ignorePatterns.size());
        } else {
            ignorePatterns = Collections.emptySet();
            logger.log(Level.INFO, "New Relic TraceIgnoreConfig: No ignore patterns configured for {0}.", PATTERNS_KEY);
        }
    }

    private static String convertWildcardToRegex(String pattern) {
        // Escape special regex characters except for '*'
        String regex = Pattern.quote(pattern).replace("\\*", ".*");
        return regex;
    }

    /**
     * Checks if a given fully qualified method name (e.g., com.example.MyClass:myMethod)
     * should be ignored for tracing based on configured patterns.
     *
     * @param fullyQualifiedMethodName The class:method name to check.
     * @return true if the method should be ignored, false otherwise.
     */
    public static boolean shouldIgnoreTrace(String fullyQualifiedMethodName) {
        if (!ignoreTracesEnabled || ignorePatterns.isEmpty()) {
            return false;
        }

        Boolean cached = decisionCache.get(fullyQualifiedMethodName);
        if (cached != null) {
            return cached;
        }

        boolean result = computeShouldIgnoreTrace(fullyQualifiedMethodName);
        decisionCache.put(fullyQualifiedMethodName, result);
        return result;
    }

    /**
     * Same check as {@link #shouldIgnoreTrace(String)}, for callers that already hold the
     * class and method names separately (every weave call site does) — avoids allocating and
     * hashing a fresh "class:method" String on every call just to use it as a cache key.
     *
     * @param className Fully qualified class name.
     * @param methodName Method name.
     * @return true if the method should be ignored, false otherwise.
     */
    public static boolean shouldIgnoreTrace(String className, String methodName) {
        if (!ignoreTracesEnabled || ignorePatterns.isEmpty()) {
            return false;
        }

        // ConcurrentHashMap rejects null keys — null class/method is an edge case,
        // not the hot path, so skip the cache rather than guard every map op.
        if (className == null || methodName == null) {
            return computeShouldIgnoreTrace(className + ":" + methodName);
        }

        ConcurrentHashMap<String, Boolean> methodCache =
            decisionCacheByClass.computeIfAbsent(className, c -> new ConcurrentHashMap<>());
        Boolean cached = methodCache.get(methodName);
        if (cached != null) {
            return cached;
        }

        boolean result = computeShouldIgnoreTrace(className + ":" + methodName);
        methodCache.put(methodName, result);
        return result;
    }

    private static boolean computeShouldIgnoreTrace(String fullyQualifiedMethodName) {
        for (Pattern pattern : ignorePatterns) {
            if (pattern.matcher(fullyQualifiedMethodName).matches()) {
                logger.log(Level.FINER, "New Relic TraceIgnoreConfig: Ignoring trace for {0} (matched pattern {1})", fullyQualifiedMethodName, pattern.pattern());
                return true;
            }
        }
        return false;
    }
}