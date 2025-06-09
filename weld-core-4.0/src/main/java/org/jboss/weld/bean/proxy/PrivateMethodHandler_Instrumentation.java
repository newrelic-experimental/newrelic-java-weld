package org.jboss.weld.bean.proxy;

import java.lang.reflect.Method;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "org.jboss.weld.bean.proxy.PrivateMethodHandler")
class PrivateMethodHandler_Instrumentation {

	@Trace
	public Object invoke(Object self, Method thisMethod, Method proceed, Object[] args) {
		if(thisMethod != null) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Weld","PrivateMethodHandler","invoke",thisMethod.getDeclaringClass().getName(),thisMethod.getName());
		}
		return Weaver.callOriginal();
	}
}
