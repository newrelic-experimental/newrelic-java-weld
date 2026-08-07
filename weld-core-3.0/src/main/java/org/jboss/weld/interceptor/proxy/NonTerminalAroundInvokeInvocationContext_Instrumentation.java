package org.jboss.weld.interceptor.proxy;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

/**
 * Registers NonTerminalAroundInvokeInvocationContext in the weave hierarchy for Weld 6.x.
 * proceed() instrumentation is inherited from AroundInvokeInvocationContext_Instrumentation.
 */
@Weave(type = MatchType.BaseClass, originalName = "org.jboss.weld.interceptor.proxy.NonTerminalAroundInvokeInvocationContext")
abstract class NonTerminalAroundInvokeInvocationContext_Instrumentation extends AroundInvokeInvocationContext_Instrumentation {
}
