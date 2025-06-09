package org.jboss.weld.bean.proxy;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.weld.core_4.WeldCoreUtils;

import jakarta.enterprise.inject.spi.Bean;

@Weave(originalName = "org.jboss.weld.bean.proxy.ProxyFactory", type = MatchType.BaseClass)
public abstract class ProxyFactory_Instrumentation<T> {

	protected abstract String getProxyNameSuffix();
	public abstract Bean<?> getBean();
	protected abstract Class<?> getProxiedBeanType();
	
	public ProxyFactory_Instrumentation(String contextId, Class<?> proxiedBeanType, Set<? extends Type> typeClosure, String proxyName,
            Bean<?> bean, boolean forceSuperClass) {
	}
	
	public T create(BeanInstance beanInstance) {
		Class<?> instanceClazz = beanInstance.getInstanceType();
		NewRelic.getAgent().getLogger().log(Level.FINE, "Instrumenting class from ProxyClass {0}, class to instrument is {1}", getClass().getName(), instanceClazz.getName());
		NewRelic.getAgent().getLogger().log(Level.FINE, "In ProxyFactory {0} create, proxy_suffix {1}", getClass().getName(), getProxyNameSuffix());

		WeldCoreUtils.instrumentClass(instanceClazz, WeldCoreUtils.BASEPROXY+getProxyNameSuffix());
		T pInstance = Weaver.callOriginal();
		return pInstance;
	}

	public Class<T> getProxyClass() {
		Class<T> clazz = Weaver.callOriginal();
		return clazz;
	}
	
//	private static void logClassMethods(String name, Class<?> clazz) {
//		Method[] methods = clazz.getDeclaredMethods();
//		StringBuffer sb = new StringBuffer();
//		Class<?>[] clazzIterfaces = clazz.getInterfaces();
//		int i = 0;
//		int size = clazzIterfaces.length;
//		sb.append("Interfaces: [ ");
//		for(Class<?> clazzInterface : clazzIterfaces) {
//			sb.append(clazzInterface.getName());
//			if(i < size - 1) {
//				sb.append(", ");
//			}
//		}
//		sb.append("], ");
//		sb.append("Methods: [ ");
//		i = 0;
//		size = methods.length;
//		for(Method method : methods) {
//			sb.append(method.toString());
//			if(i < size -1) {
//				sb.append(", ");
//			}
//			i++;
//		}
//		sb.append(']');
//		NewRelic.getAgent().getLogger().log(Level.FINE, "Declared methods for {0} with class {1} are {2}", name, clazz.getName(), sb.toString());
//	}
}
