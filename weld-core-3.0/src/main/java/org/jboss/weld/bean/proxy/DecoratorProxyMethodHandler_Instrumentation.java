package org.jboss.weld.bean.proxy;

import java.lang.reflect.Method;

import org.jboss.weld.bean.WeldDecorator;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.weld.core_3.WeldCoreUtils;

@Weave(originalName = "org.jboss.weld.bean.proxy.DecoratorProxyMethodHandler")
public class DecoratorProxyMethodHandler_Instrumentation {

	@Trace
	private Object doInvoke(WeldDecorator<?> weldDecorator, Object decoratorInstance, Method method, Object[] args) {
		if(method != null) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Weld","DecoratorProxyMethodHandler","doInvoke",WeldCoreUtils.cleanProxyClassName(method.getDeclaringClass().getName()),method.getName());
			WeldCoreUtils.addMethod(method, "Custom/Weld/DecoratorProxy/");
		}
		return Weaver.callOriginal();
	}
	
}
