package org.jboss.weld.interceptor.proxy;

import com.newrelic.agent.bridge.ExitTracer;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.weld.core_3.WeldCoreUtils;

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
