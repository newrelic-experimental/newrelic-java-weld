package org.jboss.weld.interceptor.proxy;

import java.lang.reflect.Method;

import org.jboss.weld.bean.proxy.InterceptionDecorationContext.Stack;
import org.jboss.weld.interceptor.spi.model.InterceptionType;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.ClassMethodSignatures;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.weld.config.TraceIgnoreConfig;
import com.newrelic.instrumentation.labs.weld.config.WeldTraceFilterConfig;
import com.newrelic.instrumentation.labs.weld.core_4.WeldCoreUtils;

/**
 * v2.0.5: replaced unconditional @Trace with conditional ExitTracer.
 *
 * Both invoke() and executeInterception() previously used @Trace, creating spans for
 * every CDI interceptor dispatch including high-frequency security framework classes.
 * Now both methods check the whitelist/blacklist before creating a tracer.
 */
@Weave(originalName = "org.jboss.weld.interceptor.proxy.InterceptorMethodHandler")
public class InterceptorMethodHandler_Instrumentation {

	public Object invoke(Stack stack, Object self, Method thisMethod, Method proceed,
	                     Object[] args) {
		ExitTracer tracer = null;

		if (proceed != null && isInterceptorMethod(thisMethod)) {
			String descriptor =
				"(Lorg/jboss/weld/bean/proxy/InterceptionDecorationContext$Stack;" +
				"Ljava/lang/Object;" +
				"Ljava/lang/reflect/Method;" +
				"Ljava/lang/reflect/Method;" +
				"[Ljava/lang/Object;)Ljava/lang/Object;";
			tracer = buildTracer("invoke", proceed, descriptor);
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

	protected Object executeInterception(Object instance, Method method, Method proceed,
	                                     Object[] args, InterceptionType interceptionType,
	                                     Stack stack) {
		ExitTracer tracer = null;

		if (proceed != null) {
			String descriptor =
				"(Ljava/lang/Object;" +
				"Ljava/lang/reflect/Method;" +
				"Ljava/lang/reflect/Method;" +
				"[Ljava/lang/Object;" +
				"Lorg/jboss/weld/interceptor/spi/model/InterceptionType;" +
				"Lorg/jboss/weld/bean/proxy/InterceptionDecorationContext$Stack;" +
				")Ljava/lang/Object;";
			tracer = buildTracer("executeInterception", proceed, descriptor);
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

	/**
	 * Creates an ExitTracer for the given proceed method if it passes
	 * whitelist + blacklist checks. Returns null if filtered.
	 */
	private ExitTracer buildTracer(String callerMethodName, Method proceed,
	                               String descriptor) {
		if (proceed.getDeclaringClass() == null) return null;

		String cleanedClassName = WeldCoreUtils.cleanProxyClassName(
			proceed.getDeclaringClass().getName());
		String methodName = proceed.getName();
		String fullyQualifiedMethodName = cleanedClassName + ":" + methodName;

		if (!WeldTraceFilterConfig.shouldTraceProxyCall(cleanedClassName, methodName)
				|| TraceIgnoreConfig.shouldIgnoreTrace(fullyQualifiedMethodName)) {
			return null;
		}

		String metricName = "Custom/Weld/InterceptorMethodHandler/invoke/" +
			cleanedClassName + "/" + methodName;

		ClassMethodSignature sig = new ClassMethodSignature(
			getClass().getName(), callerMethodName, descriptor);
		int index = ClassMethodSignatures.get().getIndex(sig);
		if (index == -1) {
			index = ClassMethodSignatures.get().add(sig);
		}
		if (index < 0) return null;

		return AgentBridge.instrumentation.createTracer(this, index, metricName, 0);
	}

	private boolean isInterceptorMethod(Method method) {
		return Weaver.callOriginal();
	}
}
