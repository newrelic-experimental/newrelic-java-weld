package org.jboss.weld.interceptor.proxy;

import java.lang.reflect.Method;

import org.jboss.weld.bean.proxy.InterceptionDecorationContext.Stack;
import org.jboss.weld.interceptor.spi.model.InterceptionType;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "org.jboss.weld.interceptor.proxy.InterceptorMethodHandler")
public class InterceptorMethodHandler_Instrumentation {

	@Trace
	public Object invoke(Stack stack, Object self, Method thisMethod, Method proceed, Object[] args) {
		if(proceed != null) {
			if(isInterceptorMethod(thisMethod)) {
				NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Weld","InterceptorMethodHandler","invoke",proceed.getDeclaringClass().getName(),proceed.getName());
			}
		}
		return Weaver.callOriginal();
	}
	
	protected Object executeInterception(Object instance, Method method, Method proceed, Object[] args, InterceptionType interceptionType, Stack stack) {
		if(proceed != null) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Weld","InterceptorMethodHandler","invoke",proceed.getDeclaringClass().getName(),proceed.getName());
		}
		return Weaver.callOriginal();
	}

	private boolean isInterceptorMethod(Method method) {
		return Weaver.callOriginal();
	}
	
}
