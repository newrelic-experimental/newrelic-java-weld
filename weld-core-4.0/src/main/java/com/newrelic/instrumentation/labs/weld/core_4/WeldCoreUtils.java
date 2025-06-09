package com.newrelic.instrumentation.labs.weld.core_4;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.logging.Level;

import com.newrelic.agent.bridge.AgentBridge;
import com.newrelic.api.agent.NewRelic;

public class WeldCoreUtils {

	private static final HashSet<Method> instrumented = new HashSet<Method>();
	private static final HashSet<String> ignoredMethods = new HashSet<String>();
	private static final HashSet<String> instrumentedClasses = new HashSet<String>();
	public static final String CLIENTPROXY = "/Custom/Weld/ClientProxy";
	public static final String BASEPROXY = "/Custom/Weld/";
	
	static {
		ignoredMethods.add("equals");
		ignoredMethods.add("toString");
		ignoredMethods.add("hashCode");
		ignoredMethods.add("getInstance");
	}
	
	public static void instrumentClass(Class<?> clazz, String prefix) {
		String classname = clazz.getName();
		
		if (!instrumentedClasses.contains(classname)) {
			Method[] declared = clazz.getDeclaredMethods();
			for (Method method : declared) {
				addMethod(method, prefix);
			}
			instrumentedClasses.add(classname);
		}
	}
	
	public static void addMethod(Method method, String prefix) {
		String name = method.getName();
		if(ignoredMethods.contains(name)) return;
		if(name.startsWith("lifecycle_mixin")) return;
		if(name.startsWith("weld_")) return;
		int modifiers = method.getModifiers();
		if(Modifier.isPrivate(modifiers)) return;
		
 		if(!instrumented.contains(method)) {
 			NewRelic.getAgent().getLogger().log(Level.FINER, "Instrumenting proxy method: {0}", method.toString());
			AgentBridge.instrumentation.instrument(method, prefix);
		}
	}
}
