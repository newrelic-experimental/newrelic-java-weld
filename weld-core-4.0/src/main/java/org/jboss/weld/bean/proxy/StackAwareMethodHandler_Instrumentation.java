package org.jboss.weld.bean.proxy;

import java.lang.reflect.Method;

import org.jboss.weld.bean.proxy.InterceptionDecorationContext.Stack;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "org.jboss.weld.bean.proxy.StackAwareMethodHandler", type = MatchType.Interface)
public abstract class StackAwareMethodHandler_Instrumentation {

	@Trace(dispatcher = true)
	public Object invoke(Stack stack, Object self, Method thisMethod, Method proceed, Object[] args)  {
		Class<?> methodClass = thisMethod.getDeclaringClass();
		String methodName = thisMethod.getName();
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Weld","StackAwareMethodHandler",getClass().getSimpleName(),"invoke",methodClass.getName(),methodName);
		return Weaver.callOriginal();
	}
}
