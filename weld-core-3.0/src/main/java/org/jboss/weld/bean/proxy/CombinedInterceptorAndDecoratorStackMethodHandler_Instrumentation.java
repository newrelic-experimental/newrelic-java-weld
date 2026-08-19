package org.jboss.weld.bean.proxy;

import java.lang.reflect.Method;

import org.jboss.weld.bean.proxy.InterceptionDecorationContext.Stack;
import org.jboss.weld.interceptor.proxy.InterceptorMethodHandler;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ClassMethodSignatures;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.weld.config.TraceIgnoreConfig;
import com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfig;
import com.newrelic.instrumentation.labs.weld.core_3.WeldCoreUtils;

/**
 * v2.5: replaced unconditional @Trace with conditional ExitTracer.
 *
 * Previously this class used @Trace on invoke(), which created a span for EVERY CDI
 * method call — including high-frequency security framework classes that the whitelist
 * is supposed to suppress. @Trace fires BEFORE any Java code runs, so the config was
 * never consulted.
 *
 * Fix: remove @Trace, gate ExitTracer creation behind the same whitelist + blacklist
 * checks used in BeanInstance_Instrumentation. Non-whitelisted methods produce zero
 * spans from this weave.
 */
@Weave(originalName = "org.jboss.weld.bean.proxy.CombinedInterceptorAndDecoratorStackMethodHandler")
public class CombinedInterceptorAndDecoratorStackMethodHandler_Instrumentation {

	private InterceptorMethodHandler interceptorMethodHandler = Weaver.callOriginal();

	private Object outerDecorator = Weaver.callOriginal();

	public Object invoke(Stack stack, Object self, Method thisMethod, Method proceed,
	                     Object[] args, boolean intercept, boolean popStack) {

		ExitTracer tracer = null;

		Method targetMethod = resolveTargetMethod(thisMethod, proceed, intercept);

		if (targetMethod != null && targetMethod.getDeclaringClass() != null) {
			String cleanedClassName = WeldCoreUtils.cleanProxyClassName(
				targetMethod.getDeclaringClass().getName());
			String methodName = targetMethod.getName();
			String fullyQualifiedMethodName = cleanedClassName + ":" + methodName;

			if (WeldTraceFilterConfig.shouldTraceProxyCall(cleanedClassName, methodName)
					&& !TraceIgnoreConfig.shouldIgnoreTrace(fullyQualifiedMethodName)) {

				String metricName = "Custom/Weld/StackAwareMethodHandler/" +
					"CombinedInterceptorAndDecoratorStackMethodHandler/invoke/" +
					cleanedClassName + "/" + methodName;

				String descriptor =
					"(Lorg/jboss/weld/bean/proxy/InterceptionDecorationContext$Stack;" +
					"Ljava/lang/Object;" +
					"Ljava/lang/reflect/Method;" +
					"Ljava/lang/reflect/Method;" +
					"[Ljava/lang/Object;ZZ)Ljava/lang/Object;";
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

	private static Method resolveTargetMethod(Method thisMethod, Method proceed,
	                                           boolean intercept) {
		if (intercept) {
			if (proceed != null) return proceed;
			if (thisMethod != null) return thisMethod;
		} else {
			return proceed;
		}
		return null;
	}
}
