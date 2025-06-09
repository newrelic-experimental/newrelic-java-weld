package org.jboss.weld.annotated.runtime;

import java.lang.reflect.Method;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.weld.core_4.WeldCoreUtils;

import jakarta.enterprise.inject.spi.AnnotatedMethod;

@Weave(originalName = "org.jboss.weld.annotated.runtime.InvokableAnnotatedMethod")
public abstract class InvokableAnnotatedMethod_Instrumentation<T> {

	private final AnnotatedMethod<T> annotatedMethod = Weaver.callOriginal();
	
	@Trace
	public <X> X invoke(Object instance, Object... parameters) {
		Method javaMethod = annotatedMethod.getJavaMember();
		Class<?> methodClass = javaMethod.getDeclaringClass();
		String name = javaMethod.getName();
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","InvokableAnnotatedMethod","invoke",methodClass.getName(), name);
		WeldCoreUtils.addMethod(javaMethod, "Custom/Weld/AnnotatedMethod");
		return Weaver.callOriginal();
	}
	
	@Trace
	public <X> X invokeOnInstance(Object instance, Object... parameters) {
		Method javaMethod = annotatedMethod.getJavaMember();
		Class<?> methodClass = javaMethod.getDeclaringClass();
		String name = javaMethod.getName();
		NewRelic.getAgent().getTracedMethod().setMetricName("Custom","InvokableAnnotatedMethod","invokeOnInstance",methodClass.getName(), name);
		WeldCoreUtils.addMethod(javaMethod, "Custom/Weld/AnnotatedMethod");
		return Weaver.callOriginal();
	}
}
