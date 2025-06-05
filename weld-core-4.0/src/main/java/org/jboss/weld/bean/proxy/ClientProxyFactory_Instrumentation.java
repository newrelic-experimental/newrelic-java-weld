package org.jboss.weld.bean.proxy;

import java.util.Set;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import jakarta.enterprise.inject.spi.Bean;

@Weave(originalName = "org.jboss.weld.bean.proxy.ClientProxyFactory")
public class ClientProxyFactory_Instrumentation<T> extends ProxyFactory<T> {

	public ClientProxyFactory_Instrumentation(String contextId, Class<?> proxiedBeanType, Set<? extends Type> typeClosure, Bean<?> bean) {
        super(contextId, proxiedBeanType, typeClosure, bean);
	}
	
	public T create(BeanInstance beanInstance) {
		T result =  Weaver.callOriginal();
		Class<?> proxyClass = result.getClass();
		Method[] declaredMethods = proxyClass.getDeclaredMethods();
		for(Method method : declaredMethods) {
			int mods = method.getModifiers();
			if(Modifier.isPublic(mods)) {
				AgentBridge.instrumentation.instrument(method, "Custom/Weld/ClientProxy");
			}
		}
		return result;
	}
}
