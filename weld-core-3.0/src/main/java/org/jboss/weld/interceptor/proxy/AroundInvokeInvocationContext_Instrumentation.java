package org.jboss.weld.interceptor.proxy;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.BaseClass, originalName = "org.jboss.weld.interceptor.proxy.AroundInvokeInvocationContext")
abstract class AroundInvokeInvocationContext_Instrumentation extends AbstractInvocationContext_Instrumentation {

    public Object proceed() {
    	Class<?> declaringClass = method.getDeclaringClass();
    	String methodName = method.getName();
    	TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
    	if(declaringClass != null && methodName != null) {
    		tracedMethod.setMetricName("Custom","Weld","ProxyCall",declaringClass.getName(),methodName);
    	}
    	if(target != null) {
    		tracedMethod.addCustomAttribute("cdi.interceptor.target.class", target.getClass().getName());
    	}
    	if(proceed != null) {
    		tracedMethod.addCustomAttribute("cdi.interceptor.proceed.method", proceed.toString());
    	}

    	return Weaver.callOriginal();
    }
}
