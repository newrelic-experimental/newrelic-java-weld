package org.jboss.weld.bean.proxy;

import org.jboss.weld.serialization.spi.BeanIdentifier;

import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.instrumentation.labs.weld.core_4.WeldCoreUtils;

import jakarta.enterprise.inject.spi.Bean;

@Weave(originalName = "org.jboss.weld.bean.proxy.ContextBeanInstance")
public abstract class ContextBeanInstance_Instrumentation<T>  extends AbstractBeanInstance {

	public ContextBeanInstance_Instrumentation(Bean<T> bean, BeanIdentifier id, String contextId) {
		
		Class<?> clazz = getInstanceType();
		WeldCoreUtils.instrumentClass(clazz, "Custom/Weld/ContextBean/");
	}
	
}
