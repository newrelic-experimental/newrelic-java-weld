package org.jboss.weld.interceptor.proxy;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.weld.core_4.WeldCoreUtils;

/**
 * Instruments AroundInvokeInvocationContext.proceed() to create Custom/Weld/ProxyCall spans.
 *
 * Weld calls proceed() once per interceptor in the chain, which without deduplication
 * produces N nested spans with identical metric names for the same business method.
 * WeldCoreUtils.createProxyCallTracer() applies blacklist, whitelist, and per-thread
 * deduplication so only the outermost proceed() call per method creates a tracer.
 *
 * MatchType.BaseClass covers both Weld 3.x/4.x (concrete class) and Weld 6.x
 * (abstract base — concrete subclasses inherit proceed() without overriding it).
 */
@Weave(type = MatchType.BaseClass, originalName = "org.jboss.weld.interceptor.proxy.AroundInvokeInvocationContext")
abstract class AroundInvokeInvocationContext_Instrumentation extends AbstractInvocationContext_Instrumentation {

    public Object proceed() {
        ExitTracer tracer = WeldCoreUtils.createProxyCallTracer(this, method);
        if (tracer != null && method != null && method.getDeclaringClass() != null) {
            String cleanedClassName = WeldCoreUtils.cleanProxyClassName(method.getDeclaringClass().getName());
            NewRelic.getAgent().getTracedMethod().setMetricName("Custom/Weld/ProxyCall", cleanedClassName, method.getName());
        }
        Object result = null;
        try {
            result = Weaver.callOriginal();
        } catch (Throwable t) {
            if (tracer != null) tracer.finish(t);
            throw t;
        } finally {
            if (tracer != null) WeldCoreUtils.releaseProxyCallTrace(method);
        }
        if (tracer != null) tracer.finish(0, result);
        return result;
    }
}
