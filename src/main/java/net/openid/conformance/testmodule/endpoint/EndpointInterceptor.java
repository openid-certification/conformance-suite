package net.openid.conformance.testmodule.endpoint;

/**
 * Wraps the handling of every request, matched or not, so that the steps each module performs
 * around its endpoints (mapping the incoming request, test status changes, TLS checks) are
 * written once. Interceptors run in registration order; each calls {@link EndpointChain#proceed}
 * to continue, or returns a response of its own to short-circuit the handler.
 *
 * <p>The route is resolved before the first interceptor runs: {@link EndpointRequest#route()} and
 * {@link EndpointRequest#unmatched()} tell an interceptor which endpoint the request is for.
 */
@FunctionalInterface
public interface EndpointInterceptor {

	Object intercept(EndpointRequest request, EndpointChain chain);
}
