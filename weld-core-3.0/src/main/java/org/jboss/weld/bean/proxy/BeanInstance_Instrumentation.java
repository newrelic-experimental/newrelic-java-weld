package org.jboss.weld.bean.proxy;

import java.lang.reflect.Method;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "org.jboss.weld.bean.proxy.BeanInstance", type = MatchType.Interface)
public class BeanInstance_Instrumentation {

	@Trace(dispatcher = true)
	public Object invoke(Object instance, Method method, Object... arguments) {
		if(method != null) {
			String methodName = method.getName();
			Class<?> methodClass = method.getDeclaringClass();
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Weld","BeanInstance",getClass().getSimpleName(),"invoke", methodClass.getName(), methodName);
		}
		return Weaver.callOriginal();
	}
}
