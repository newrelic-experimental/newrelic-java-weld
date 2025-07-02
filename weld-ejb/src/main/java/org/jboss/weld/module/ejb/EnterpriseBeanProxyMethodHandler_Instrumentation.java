package org.jboss.weld.module.ejb;

import java.lang.reflect.Method;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.weld.ejb.WeldEJBUtils;

@Weave(originalName = "org.jboss.weld.module.ejb.EnterpriseBeanProxyMethodHandler")
class EnterpriseBeanProxyMethodHandler_Instrumentation<T> {

	
	public Object invoke(Object self, Method method, Method proceed, Object[] args)  {
		WeldEJBUtils.addMethod(method, "Custom/Weld/EJB/");
		return Weaver.callOriginal();
	}
}
