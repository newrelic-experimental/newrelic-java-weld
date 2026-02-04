package org.jboss.weld.interceptor.proxy;

import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ClassMethodSignatures;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.weld.config.TraceIgnoreConfig;
import com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfig;
import com.newrelic.instrumentation.labs.weld.core_4.WeldCoreUtils;

@Weave(type = MatchType.BaseClass, originalName = "org.jboss.weld.interceptor.proxy.AroundInvokeInvocationContext")
abstract class AroundInvokeInvocationContext_Instrumentation extends AbstractInvocationContext_Instrumentation {

    public Object proceed() {
    	ExitTracer tracer = null;

    	// Conditional tracer creation based on filters
    	// Note: ejb-4.0 module has @Trace(dispatcher=true) on this method via jakarta.interceptor.InvocationContext
    	if (method != null) {
    		Class<?> declaringClass = method.getDeclaringClass();
    		String methodName = method.getName();

    		if (declaringClass != null && methodName != null) {
    			String className = declaringClass.getName();
    			String cleanedClassName = WeldCoreUtils.cleanProxyClassName(className);
    			String fullyQualifiedMethodName = cleanedClassName + ":" + methodName;

    			// FILTER 1: Check blacklist (ignore patterns)
    			boolean isBlacklisted = TraceIgnoreConfig.shouldIgnoreTrace(fullyQualifiedMethodName);
    			if (isBlacklisted) {
    				NewRelic.getAgent().getLogger().log(Level.FINEST,
    					"Skipping ProxyCall trace (blacklisted): {0}", fullyQualifiedMethodName);
    			}

    			// FILTER 2: Check whitelist (trace filter)
    			boolean isWhitelisted = WeldTraceFilterConfig.shouldTraceProxyCall(cleanedClassName, methodName);
    			if (!isBlacklisted && !isWhitelisted) {
    				NewRelic.getAgent().getLogger().log(Level.FINEST,
    					"Skipping ProxyCall trace (not whitelisted): {0}", fullyQualifiedMethodName);
    			}

    			// Create segment and child tracer if passed both filters
    			if (!isBlacklisted && isWhitelisted) {
    				// STEP 1: Set segment name (for the tracer created by ejb-4.0's @Trace)
    				// This names the segment in the transaction trace
    				NewRelic.getAgent().getTracedMethod().setMetricName("Custom/Weld/ProxyCall", cleanedClassName, methodName);

    				// STEP 2: Create child tracer for detailed timing/metrics
    				String descriptor = "()Ljava/lang/Object;";
    				ClassMethodSignature signature = new ClassMethodSignature(
    					getClass().getName(),
    					"proceed",
    					descriptor
    				);

    				int index = ClassMethodSignatures.get().getIndex(signature);
    				if (index == -1) {
    					index = ClassMethodSignatures.get().add(signature);
    				}

    				if (index >= 0) {
    					// Build metric name for child tracer
    					String metricName = String.format(
    						"Custom/Weld/ProxyCall/%s/%s",
    						cleanedClassName,
    						methodName
    					);

    					// Create child tracer (no LEAF flag = shows as segment in transaction trace)
    					int tracerFlags = 0;  // No flags = child tracer
    					tracer = AgentBridge.instrumentation.createTracer(this, index, metricName, tracerFlags);
    				}
    			}
    			// If filtered out (blacklisted or not whitelisted):
    			// - tracer remains null (no memory overhead)
    			// - transaction continues normally
    			// - no metrics created for this method
    		}
    	}

    	// Call original method (ONLY ONCE!)
    	Object result = null;
    	try {
    		result = Weaver.callOriginal();
    	} catch (Throwable t) {
    		if (tracer != null) {
    			tracer.finish(t);
    		}
    		throw t;
    	}

    	// Finish tracer on success
    	if (tracer != null) {
    		tracer.finish(0, result);
    	}

    	return result;
    }
}
