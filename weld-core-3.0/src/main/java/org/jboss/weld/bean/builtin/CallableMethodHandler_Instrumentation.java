package org.jboss.weld.bean.builtin;

import java.lang.reflect.Method;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "org.jboss.weld.bean.builtin.CallableMethodHandler")
public class CallableMethodHandler_Instrumentation {

	@Trace
	public Object invoke(Object self, Method proxiedMethod, Method proceed, Object[] args) {
		if(proxiedMethod != null) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Weld","CallableMethodHandler","invoke",proxiedMethod.getDeclaringClass().getName(),proxiedMethod.getName());
		}
		return Weaver.callOriginal();
	}
}
