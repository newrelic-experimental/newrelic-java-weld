package org.jboss.weld.bean.proxy;

import java.lang.reflect.Method;

import org.jboss.weld.bean.proxy.InterceptionDecorationContext.Stack;
import org.jboss.weld.interceptor.proxy.InterceptorMethodHandler;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.weld.core_3.WeldCoreUtils;

@Weave(originalName = "org.jboss.weld.bean.proxy.CombinedInterceptorAndDecoratorStackMethodHandler")
public class CombinedInterceptorAndDecoratorStackMethodHandler_Instrumentation {
	
	private InterceptorMethodHandler interceptorMethodHandler = Weaver.callOriginal();
	
	private Object outerDecorator = Weaver.callOriginal();

	@Trace
	public Object invoke(Stack stack, Object self, Method thisMethod, Method proceed, Object[] args, boolean intercept, boolean popStack) {
		if(intercept) {
			if(interceptorMethodHandler == null) {
				if(outerDecorator != null) {
					if(thisMethod != null) {
						NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Weld","CombinedInterceptorAndDecoratorStackMethodHandler","invoke",WeldCoreUtils.cleanProxyClassName(thisMethod.getDeclaringClass().getName()),thisMethod.getName());
						WeldCoreUtils.addMethod(thisMethod, "Custom/Weld/Interceptor_Decorator/Stack");
					}
				} else  if(proceed != null){
					NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Weld","CombinedInterceptorAndDecoratorStackMethodHandler","invoke",WeldCoreUtils.cleanProxyClassName(proceed.getDeclaringClass().getName()),proceed.getName());
					WeldCoreUtils.addMethod(thisMethod, "Custom/Weld/Interceptor_Decorator/Stack");
				}
			}
		} else {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Weld","CombinedInterceptorAndDecoratorStackMethodHandler","invoke",WeldCoreUtils.cleanProxyClassName(proceed.getDeclaringClass().getName()),proceed.getName());
			WeldCoreUtils.addMethod(thisMethod, "Custom/Weld/Interceptor_Decorator/Stack");
		}
		return Weaver.callOriginal();
	}
}
