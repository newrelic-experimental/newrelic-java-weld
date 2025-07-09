// src/main/java/com/nr/instrumentation/weld/custom/InvocationContext_CustomMetric.java
package javax.interceptor; // Your custom package

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.TracedMethod;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;
import com.newrelic.instrumentation.labs.weld.core_3.WeldCoreUtils;

import java.lang.reflect.Method; // Needed to get method details


// Target the same class and method as the built-in instrumentation
@Weave(originalName = "javax.interceptor.InvocationContext", type = MatchType.Interface)
public abstract class InvocationContext_Instrumenation {

    // These abstract methods shadow the original methods of InvocationContext,
    // allowing you to call them from your weave code.
    public abstract Object getTarget();
    public abstract Method getMethod(); // Crucial to get the intercepted method
    public abstract Object[] getParameters(); // Optional, if you want parameter details

    @Trace(dispatcher = true) // Keep this to ensure it's still considered a transaction dispatcher if applicable
    public Object proceed() throws Exception {
        // 1. Call the original method first.
        // This executes the New Relic agent's default instrumentation for InvocationContext.proceed()
        // and then the actual method execution.
        Object result = Weaver.callOriginal();

        // 2. After the original method (and any previous instrumentation) has run,
        //    get the current TracedMethod and modify its name.
        TracedMethod tracedMethod = NewRelic.getAgent().getTracedMethod();
        if (tracedMethod != null) {
            Object target = getTarget(); // The bean instance being intercepted
            Method interceptedMethod = getMethod(); // The method being intercepted

            String targetClassName = "UnknownClass";
            String methodName = "unknownMethod";

            if (target != null) {
                targetClassName = target.getClass().getName();
            }
            if (interceptedMethod != null) {
                methodName = interceptedMethod.getName();
            }

            // Construct your new, more specific metric name
            // Example: Custom/Weld/Interceptor/com.example.MyEJBService/myEjbMethod
            String customMetricName = "Custom"+ "/"+ "Weld"+ "/" +"ProxyCall" +"/"+ WeldCoreUtils.cleanProxyClassName(targetClassName) + "/" + methodName;

            // Set the new metric name. This will overwrite any name set by the default agent
            // for this specific segment/transaction.
            tracedMethod.setMetricName(customMetricName);

            // You can also add more custom attributes for richer data
            tracedMethod.addCustomAttribute("cdi.interceptor.target.class", targetClassName);
            tracedMethod.addCustomAttribute("cdi.interceptor.target.method", methodName);
            // tracedMethod.addCustomAttribute("cdi.interceptor.parameters", java.util.Arrays.toString(getParameters())); // If you want parameters

            //WeldInstrumentationUtility.log(java.util.logging.Level.FINEST, "Custom metric name set for CDI EJB interceptor: {0}", customMetricName);
        } else {
          //  WeldInstrumentationUtility.log(java.util.logging.Level.FINE, "TracedMethod is null for InvocationContext.proceed(), cannot apply custom metric name.");
        }

        return result; // Return the result from the original method call
    }
}