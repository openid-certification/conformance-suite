package net.openid.conformance.testmodule.endpoint;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Routes a module's incoming requests to declared endpoint handlers. A module builds one during
 * configuration and calls {@link #dispatch} from {@code handleHttp}, {@code handleHttpMtls} and
 * {@code handleWellKnown}:
 *
 * <pre>{@code
 * endpoints = EndpointDispatcher.builder()
 *     .endpoints(new FAPI2AuthorizationServerEndpoints(this, profileBehavior))
 *     .interceptor(new StoreIncomingRequestInterceptor(env))
 *     .onUnmatched(r -> unexpectedHttpRequest(r.path(), r.requestParts()))
 *     .build();
 *
 * public Object handleHttp(String path, HttpServletRequest req, HttpServletResponse res,
 *                          HttpSession session, JsonObject requestParts) {
 *     return endpoints.dispatch(Channel.HTTP, path, req, res, session, requestParts);
 * }
 * }</pre>
 */
public final class EndpointDispatcher {

	private final EndpointRegistry registry;
	private final List<EndpointInterceptor> interceptors;
	private final UnmatchedEndpointHandler unmatchedHandler;

	private EndpointDispatcher(EndpointRegistry registry, List<EndpointInterceptor> interceptors,
			UnmatchedEndpointHandler unmatchedHandler) {
		this.registry = registry;
		this.interceptors = List.copyOf(interceptors);
		this.unmatchedHandler = unmatchedHandler;
	}

	public static Builder builder() {
		return new Builder();
	}

	public EndpointRegistry registry() {
		return registry;
	}

	public Object dispatch(Channel channel, String path, HttpServletRequest servletRequest,
			HttpServletResponse servletResponse, HttpSession session, JsonObject requestParts) {
		return dispatch(EndpointRequest.of(channel, path, servletRequest, servletResponse, session, requestParts));
	}

	public Object dispatch(EndpointRequest request) {
		EndpointRegistry.Resolution resolution = registry.resolve(request.channel(), request.path(), request.method());
		return proceed(request.withResolution(resolution.route(), resolution.unmatched()), 0);
	}

	private Object proceed(EndpointRequest request, int index) {
		if (index < interceptors.size()) {
			return interceptors.get(index).intercept(request, next -> proceed(next, index + 1));
		}
		return request.route()
			.map(route -> route.invoke(request))
			.orElseGet(() -> unmatchedHandler.handle(request));
	}

	public static final class Builder {

		private final List<Object> endpoints = new ArrayList<>();
		private final List<EndpointInterceptor> interceptors = new ArrayList<>();
		private UnmatchedEndpointHandler unmatchedHandler;

		private Builder() {
		}

		/**
		 * Objects whose annotated methods handle requests. Two objects may not declare the same endpoint.
		 */
		public Builder endpoints(Object... targets) {
			endpoints.addAll(List.of(targets));
			return this;
		}

		/**
		 * Add an interceptor; interceptors run in the order added, outermost first.
		 */
		public Builder interceptor(EndpointInterceptor interceptor) {
			interceptors.add(interceptor);
			return this;
		}

		public Builder onUnmatched(UnmatchedEndpointHandler handler) {
			this.unmatchedHandler = handler;
			return this;
		}

		public EndpointDispatcher build() {
			if (unmatchedHandler == null) {
				throw new IllegalStateException("An EndpointDispatcher needs an unmatched-request handler");
			}
			return new EndpointDispatcher(EndpointRegistry.scan(endpoints), interceptors, unmatchedHandler);
		}
	}
}
