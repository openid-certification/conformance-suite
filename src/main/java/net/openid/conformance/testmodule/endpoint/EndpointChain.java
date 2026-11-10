package net.openid.conformance.testmodule.endpoint;

/**
 * The rest of the interceptor chain, ending in the endpoint handler or the unmatched-request handler.
 */
@FunctionalInterface
public interface EndpointChain {

	/**
	 * Continue with the given request, which may be a copy carrying extra state such as
	 * {@link EndpointRequest#withRequestId(String)}. The route is not re-resolved.
	 */
	Object proceed(EndpointRequest request);
}
