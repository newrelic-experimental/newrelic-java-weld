package org.jboss.weld.bean.proxy;

import java.lang.reflect.Method;
import java.text.MessageFormat;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ClassMethodSignatures;
import com.newrelic.agent.tracers.TracerFlags;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.weld.config.TraceIgnoreConfig;
import com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfig;
import com.newrelic.instrumentation.labs.weld.core_3.WeldCoreUtils;

@Weave(originalName = "org.jboss.weld.bean.proxy.BeanInstance", type = MatchType.Interface)
public class BeanInstance_Instrumentation {

	public Object invoke(Object instance, Method method, Object... arguments) {
		// BeanInstance.invoke() may or may not have a parent tracer on the call stack
		// We'll try to set the metric name if a tracer exists, and create a child tracer
		ExitTracer tracer = null;

		// Conditional tracer creation based on whitelist filter
		if (method != null) {
			String className = method.getDeclaringClass().getName();
			String cleanedClassName = WeldCoreUtils.cleanProxyClassName(className);
			String methodName = method.getName();

			String fullyQualifiedMethodName = cleanedClassName + ":" + methodName;

			// Whitelist first — fast rejection. Blacklist only runs for whitelisted methods.
			if (WeldTraceFilterConfig.shouldTraceBeanInstance(cleanedClassName, methodName)
					&& !TraceIgnoreConfig.shouldIgnoreTrace(fullyQualifiedMethodName)) {
				// Build metric name with cleaned class name
				String metricName = MessageFormat.format(
					"Custom/Weld/BeanInstance/{0}/invoke/{1}/{2}",
					getClass().getSimpleName(),
					cleanedClassName,
					methodName
				);

				// Try to set metric name on parent tracer (if one exists)
				try {
					AgentBridge.getAgent().getTracedMethod().setMetricName("Custom/Weld/BeanInstance",
						getClass().getSimpleName() + "/invoke/" + cleanedClassName, methodName);
				} catch (Exception e) {
					// No parent tracer, that's okay
				}

				// Create child tracer for detailed timing
				String descriptor = "(Ljava/lang/Object;Ljava/lang/reflect/Method;[Ljava/lang/Object;)Ljava/lang/Object;";
				ClassMethodSignature signature = new ClassMethodSignature(
					getClass().getName(),
					"invoke",
					descriptor
				);

				int index = ClassMethodSignatures.get().getIndex(signature);
				if (index == -1) {
					index = ClassMethodSignatures.get().add(signature);
				}

				if (index >= 0) {
					// Create child tracer (no LEAF flag = shows as segment in transaction trace)
					int tracerFlags = 0;  // No flags = child tracer
					tracer = AgentBridge.instrumentation.createTracer(this, index, metricName, tracerFlags);
				}
			}
		}

		// Call original method
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
