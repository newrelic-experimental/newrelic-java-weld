package com.newrelic.instrumentation.labs.weld.core_4;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.instrumentation.labs.weld.config.TraceIgnoreConfig;
import com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfig;

public class WeldCoreUtils {

	private static final HashSet<Method> instrumented = new HashSet<Method>();
	private static final HashSet<String> ignoredMethods = new HashSet<String>();
	private static final HashSet<String> instrumentedClasses = new HashSet<String>();
	public static final String CLIENTPROXY = "/Custom/Weld/ClientProxy";
	public static final String BASEPROXY = "/Custom/Weld/";
	
	
	   private static final String PROXY_SUFFIX_REGEX = "\\$.*?Subclass"; // Matches $...Subclass (case-insensitive, non-greedy)

	    /**
	     * Cleans up a class name by removing Weld-specific proxy suffixes (e.g., "$Proxy$_$$_WeldSubclass").
	     * The pattern used is case-insensitive and removes the entire string starting from the first '$'
	     * if it contains "Subclass" (upper or lower case).
	     *
	     * @param className The original class name, potentially from a Weld proxy.
	     * @return The cleaned class name.
	     */
	    public static String cleanProxyClassName(String className) {
	        if (className == null || className.isEmpty()) {
	            return className;
	        }

	        // Check for '$' to optimize. If no '$', no proxy suffix is present.
	        int dollarIndex = className.indexOf('$');
	        if (dollarIndex == -1) {
	            return className;
	        }

	        // Use regex for robust and case-insensitive matching
	        String cleanedName = className.replaceAll(PROXY_SUFFIX_REGEX, "");

	        // Log if a change was made for debugging
	        if (!cleanedName.equals(className)) {
	            NewRelic.getAgent().getLogger().log(Level.FINEST, "Cleaned Weld proxy class name: '{0}' -> '{1}'", className, cleanedName);
	        }

	        return cleanedName;
	    }
	    
	static {
		ignoredMethods.add("equals");
		ignoredMethods.add("toString");
		ignoredMethods.add("hashCode");
		ignoredMethods.add("getInstance");
	}
	
	public static void instrumentClass(Class<?> clazz, String prefix) {
		String classname = clazz.getName();
		
		if (!instrumentedClasses.contains(classname)) {
			Method[] declared = clazz.getDeclaredMethods();
			for (Method method : declared) {
				addMethod(method, prefix);
			}
			instrumentedClasses.add(classname);
		}
	}
	
	public static void addMethod(Method method, String prefix) {
		String name = method.getName();
		if(ignoredMethods.contains(name)) return;
		if(name.startsWith("lifecycle_mixin")) return;
		if(name.startsWith("weld_")) return;
		int modifiers = method.getModifiers();
		if(Modifier.isPrivate(modifiers)) return;

		String className = method.getDeclaringClass().getName();
		String cleanedClassName = cleanProxyClassName(className);
		String methodName = method.getName();
		String fullyQualifiedMethodName = cleanedClassName + ":" + methodName;

		// FILTER 1: Check blacklist (existing - backward compatible)
		// Skip methods explicitly ignored by customer
        if (TraceIgnoreConfig.shouldIgnoreTrace(fullyQualifiedMethodName)) {
            NewRelic.getAgent().getLogger().log(Level.FINEST,
				"Skipping instrumentation (blacklisted): {0}", fullyQualifiedMethodName);
            return; // Skip instrumentation
        }

		// FILTER 2: Check whitelist (new enhancement)
		// Determine filter type based on prefix:
		// - ContextBean uses BeanInstance filtering
		// - All others use ProxyCall filtering
		boolean isContextBean = prefix != null && prefix.contains("/ContextBean/");
		boolean shouldTrace;
		String instrumentationType;

		if (isContextBean) {
			shouldTrace = WeldTraceFilterConfig.shouldTraceBeanInstance(cleanedClassName, methodName);
			instrumentationType = "ContextBean";
		} else {
			shouldTrace = WeldTraceFilterConfig.shouldTraceProxyCall(cleanedClassName, methodName);
			instrumentationType = "ProxyCall";
		}

		if (!shouldTrace) {
			NewRelic.getAgent().getLogger().log(Level.FINEST,
				"Skipping {0} instrumentation (not whitelisted): {1}", instrumentationType, fullyQualifiedMethodName);
			return; // Skip instrumentation
		}

 		if(!instrumented.contains(method)) {
 			NewRelic.getAgent().getLogger().log(Level.FINER, "Instrumenting {0} method: {1}", instrumentationType, method.toString());
			// Build metric name with cleaned class name
			String metricName = prefix + cleanedClassName + "/" + methodName;
			AgentBridge.instrumentation.instrument(method, metricName);
            instrumented.add(method); // Add to instrumented set AFTER successful instrumentation
		}
	}
	
	
	
}
