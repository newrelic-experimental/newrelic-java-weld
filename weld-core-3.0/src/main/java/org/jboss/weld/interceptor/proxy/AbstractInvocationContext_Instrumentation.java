package org.jboss.weld.interceptor.proxy;

import java.lang.reflect.Method;
import java.util.Map;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

@Weave(type = MatchType.BaseClass, originalName = "org.jboss.weld.interceptor.proxy.AbstractInvocationContext")
class AbstractInvocationContext_Instrumentation {

    protected Map<String, Object> contextData = Weaver.callOriginal();
    protected final Method method = Weaver.callOriginal();
    protected final Object target = Weaver.callOriginal();
    protected final Method proceed = Weaver.callOriginal();

}
