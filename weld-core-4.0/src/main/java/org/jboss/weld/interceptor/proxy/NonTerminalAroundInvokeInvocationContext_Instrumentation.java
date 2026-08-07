package org.jboss.weld.interceptor.proxy;

import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;

/**
 * Registers NonTerminalAroundInvokeInvocationContext in the weave hierarchy for Weld 6.x.
 *
 * In Weld 6.x, AroundInvokeInvocationContext is abstract with two concrete subclasses:
 * NonTerminalAroundInvokeInvocationContext and TerminalAroundInvokeInvocationContext.
 * Neither subclass overrides proceed() — the instrumentation from
 * AroundInvokeInvocationContext_Instrumentation applies via inheritance.
 */
@Weave(type = MatchType.BaseClass, originalName = "org.jboss.weld.interceptor.proxy.NonTerminalAroundInvokeInvocationContext")
abstract class NonTerminalAroundInvokeInvocationContext_Instrumentation extends AroundInvokeInvocationContext_Instrumentation {
}
