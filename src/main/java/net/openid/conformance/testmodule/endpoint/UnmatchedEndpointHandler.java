package net.openid.conformance.testmodule.endpoint;

/**
 * Answers a request that resolved to no endpoint, typically by delegating to the module's
 * {@code unexpectedHttpRequest} or to a superclass's {@code handleHttp}.
 * {@link EndpointRequest#unmatched()} says why the request did not match.
 */
@FunctionalInterface
public interface UnmatchedEndpointHandler {

	Object handle(EndpointRequest request);
}
