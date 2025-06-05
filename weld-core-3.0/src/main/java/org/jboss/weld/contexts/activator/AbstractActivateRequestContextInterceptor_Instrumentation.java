package org.jboss.weld.contexts.activator;

import java.lang.reflect.Method;

import javax.interceptor.InvocationContext;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;


@Weave(originalName = "org.jboss.weld.contexts.activator.AbstractActivateRequestContextInterceptor", type = MatchType.BaseClass)
public abstract class AbstractActivateRequestContextInterceptor_Instrumentation {

	@Trace(dispatcher = true)
	Object invoke(InvocationContext ctx)  {
		Method method = ctx.getMethod();
		if(method != null) {
			NewRelic.getAgent().getTracedMethod().setMetricName("Custom","Weld","AbstractActivateRequestContextInterceptor",getClass().getSimpleName(),method.getDeclaringClass().getName(),method.getName());
		}
		return Weaver.callOriginal();
	}
}
