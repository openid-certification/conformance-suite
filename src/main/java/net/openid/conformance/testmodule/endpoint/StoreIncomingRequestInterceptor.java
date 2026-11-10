package net.openid.conformance.testmodule.endpoint;

import net.openid.conformance.testmodule.Environment;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Stores each request's parts in the environment under a fresh {@code incoming_request_<random>} key
 * and passes the key on as {@link EndpointRequest#requestId()}, as the modules' {@code handleHttp}
 * overrides do. A unique key per request keeps concurrent requests from overwriting each other; mapping
 * a well-known name such as {@code incoming_request} onto it stays the handler's job, since it must
 * only last while the handler holds the test lock.
 */
public final class StoreIncomingRequestInterceptor implements EndpointInterceptor {

	private final Environment env;

	public StoreIncomingRequestInterceptor(Environment env) {
		this.env = env;
	}

	@Override
	public Object intercept(EndpointRequest request, EndpointChain chain) {
		String requestId = "incoming_request_" + RandomStringUtils.secure().nextAlphanumeric(37);
		env.putObject(requestId, request.requestParts());
		return chain.proceed(request.withRequestId(requestId));
	}
}
