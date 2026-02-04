package com.newrelic.weld.test.web;

import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.util.logging.Logger;

/**
 * Sample CDI Interceptor for testing AroundInvokeInvocationContext instrumentation.
 *
 * This interceptor will be invoked for any method annotated with @Logged,
 * which will trigger the Weld AroundInvokeInvocationContext.proceed() method
 * and allow the New Relic instrumentation to create Custom/Weld/ProxyCall traces.
 */
@Logged
@Interceptor
@Priority(Interceptor.Priority.APPLICATION)
public class LoggingInterceptor {

    private static final Logger LOGGER = Logger.getLogger(LoggingInterceptor.class.getName());

    /**
     * Intercepts method invocations.
     * The context.proceed() call will trigger AroundInvokeInvocationContext.proceed()
     * which is instrumented by New Relic.
     */
    @AroundInvoke
    public Object logMethodInvocation(InvocationContext context) throws Exception {
        String className = context.getTarget().getClass().getSimpleName();
        String methodName = context.getMethod().getName();

        LOGGER.info(">>> Intercepting: " + className + "." + methodName + "()");

        long startTime = System.nanoTime();

        try {
            // This proceed() call goes through Weld's AroundInvokeInvocationContext
            // which should now be traced by New Relic
            Object result = context.proceed();

            long duration = (System.nanoTime() - startTime) / 1_000_000;
            LOGGER.info("<<< Completed: " + className + "." + methodName + "() in " + duration + "ms");

            return result;
        } catch (Exception e) {
            LOGGER.severe("!!! Exception in: " + className + "." + methodName + "() - " + e.getMessage());
            throw e;
        }
    }
}
