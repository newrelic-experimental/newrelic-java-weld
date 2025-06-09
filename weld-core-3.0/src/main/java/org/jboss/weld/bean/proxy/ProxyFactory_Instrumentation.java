package org.jboss.weld.bean.proxy;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.logging.Level;

import javax.enterprise.inject.spi.Bean;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.NewField;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(originalName = "org.jboss.weld.bean.proxy.ProxyFactory", type = MatchType.BaseClass)
public abstract class ProxyFactory_Instrumentation<T> {

	@NewField
	protected Class<?> proxiedClassToUse = null;

	protected abstract String getProxyNameSuffix();
	public abstract Bean<?> getBean();
	
	 public ProxyFactory_Instrumentation(String contextId, Class<?> proxiedBeanType, Set<? extends Type> typeClosure, String proxyName, Bean<?> bean, boolean forceSuperClass) {
		 proxiedClassToUse = proxiedBeanType;
	 }

	public Class<T> getProxyClass() {
		Bean<?> tempBean = getBean();
		Class<?> classToUse = tempBean != null ? tempBean.getBeanClass() : proxiedClassToUse;

		Class<T> theClass = Weaver.callOriginal();

		Method[] origMethods = classToUse.getDeclaredMethods();
		NewRelic.getAgent().getLogger().log(Level.FINE, "Methods from original class: {0}", origMethods);
		Method[] proxyMethods = theClass.getDeclaredMethods();
		NewRelic.getAgent().getLogger().log(Level.FINE, "Methods from proxy class: {0}", proxyMethods);
		
		for(Method method : origMethods) {
			try {
				Method proxiedMethod = theClass.getDeclaredMethod(method.getName(), method.getParameterTypes());
				if(proxiedMethod != null) {
					AgentBridge.instrumentation.instrument(proxiedMethod, "Custom/Weld/Proxy/"+getProxyNameSuffix());
				}
			} catch (NoSuchMethodException e) {
				NewRelic.getAgent().getLogger().log(Level.FINEST, e, "Did not find method {0} in proxy class {1}", method.getName(),theClass.getName());
			} catch (SecurityException e) {
				NewRelic.getAgent().getLogger().log(Level.FINEST, e, "Could not access method {0} in proxy class {1}", method.getName(),theClass.getName());
			}

		}

		return theClass;
	}
}
