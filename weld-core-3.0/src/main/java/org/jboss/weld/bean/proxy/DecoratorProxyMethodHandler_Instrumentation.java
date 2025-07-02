package org.jboss.weld.bean.proxy;

import java.lang.reflect.Method;

import org.jboss.weld.bean.WeldDecorator;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "org.jboss.weld.bean.proxy.DecoratorProxyMethodHandler")
public class DecoratorProxyMethodHandler_Instrumentation {

	@Trace
	private Object doInvoke(WeldDecorator<?> weldDecorator, Object decoratorInstance, Method method, Object[] args) {
		if(method != null) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Weld","DecoratorProxyMethodHandler","doInvoke",method.getDeclaringClass().getName(),method.getName());
		}
		return Weaver.callOriginal();
	}
	
}
