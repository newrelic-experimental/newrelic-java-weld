package org.jboss.weld.bean.proxy;

import java.lang.reflect.Method;

import org.jboss.weld.bean.proxy.InterceptionDecorationContext.Stack;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ClassMethodSignatures;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.weld.config.TraceIgnoreConfig;
import com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfig;
import com.newrelic.instrumentation.labs.weld.core_3.WeldCoreUtils;

/**
 * v2.0.5: replaced unconditional @Trace with conditional ExitTracer.
 *
 * StackAwareMethodHandler is the interface implemented by
 * CombinedInterceptorAndDecoratorStackMethodHandler. The @Trace annotation fired for
 * EVERY CDI call. The new implementation checks whitelist + blacklist before creating
 * a tracer, eliminating spans for high-frequency non-whitelisted classes.
 */
@Weave(originalName = "org.jboss.weld.bean.proxy.StackAwareMethodHandler", type = MatchType.Interface)
public abstract class StackAwareMethodHandler_Instrumentation {

	public Object invoke(Stack stack, Object self, Method thisMethod, Method proceed,
	                     Object[] args) {
		ExitTracer tracer = null;

		// Use thisMethod as the target when no proceed reference
		Method targetMethod = (thisMethod != null) ? thisMethod : proceed;

		if (targetMethod != null && targetMethod.getDeclaringClass() != null) {
			String cleanedClassName = WeldCoreUtils.cleanProxyClassName(
				targetMethod.getDeclaringClass().getName());
			String methodName = targetMethod.getName();
			String fullyQualifiedMethodName = cleanedClassName + ":" + methodName;

			if (WeldTraceFilterConfig.shouldTraceProxyCall(cleanedClassName, methodName)
					&& !TraceIgnoreConfig.shouldIgnoreTrace(fullyQualifiedMethodName)) {

				String metricName = "Custom/Weld/StackAwareMethodHandler/" +
					getClass().getSimpleName() + "/invoke/" +
					cleanedClassName + "/" + methodName;

				String descriptor =
					"(Lorg/jboss/weld/bean/proxy/InterceptionDecorationContext$Stack;" +
					"Ljava/lang/Object;" +
					"Ljava/lang/reflect/Method;" +
					"Ljava/lang/reflect/Method;" +
					"[Ljava/lang/Object;)Ljava/lang/Object;";
				ClassMethodSignature sig = new ClassMethodSignature(
					getClass().getName(), "invoke", descriptor);
				int index = ClassMethodSignatures.get().getIndex(sig);
				if (index == -1) {
					index = ClassMethodSignatures.get().add(sig);
				}
				if (index >= 0) {
					tracer = AgentBridge.instrumentation.createTracer(
						this, index, metricName, 0);
				}
			}
		}

		Object result = null;
		try {
			result = Weaver.callOriginal();
		} catch (Throwable t) {
			if (tracer != null) {
				tracer.finish(t);
			}
			throw t;
		}

		if (tracer != null) {
			tracer.finish(0, result);
		}
		return result;
	}
}
