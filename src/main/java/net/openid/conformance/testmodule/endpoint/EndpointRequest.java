package net.openid.conformance.testmodule.endpoint;

import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.Locale;
import java.util.Optional;

/**
 * A request on its way through an {@link EndpointDispatcher}: what the module's {@code handleHttp}
 * received, plus the resolved route and the environment key of the stored request once an
 * interceptor has stored it. Instances are immutable; the {@code with...} methods return copies.
 */
public final class EndpointRequest {

	private final Channel channel;
	private final String path;
	private final HttpServletRequest servletRequest;
	private final HttpServletResponse servletResponse;
	private final HttpSession session;
	private final JsonObject requestParts;
	private final String requestId;
	private final EndpointRoute route;
	private final Unmatched unmatched;

	private EndpointRequest(Channel channel, String path, HttpServletRequest servletRequest,
			HttpServletResponse servletResponse, HttpSession session, JsonObject requestParts,
			String requestId, EndpointRoute route, Unmatched unmatched) {
		this.channel = channel;
		this.path = path;
		this.servletRequest = servletRequest;
		this.servletResponse = servletResponse;
		this.session = session;
		this.requestParts = requestParts;
		this.requestId = requestId;
		this.route = route;
		this.unmatched = unmatched;
	}

	public static EndpointRequest of(Channel channel, String path, HttpServletRequest servletRequest,
			HttpServletResponse servletResponse, HttpSession session, JsonObject requestParts) {
		return new EndpointRequest(channel, path, servletRequest, servletResponse, session, requestParts,
			null, null, null);
	}

	public Channel channel() {
		return channel;
	}

	/**
	 * The path exactly as the module received it.
	 */
	public String path() {
		return path;
	}

	/**
	 * The upper-case HTTP method, from the request parts {@code TestDispatcher} builds, or from the
	 * servlet request when no request parts are present.
	 */
	public String method() {
		if (requestParts != null && requestParts.has("method")) {
			return OIDFJSON.getString(requestParts.get("method"));
		}
		return servletRequest != null ? servletRequest.getMethod().toUpperCase(Locale.ROOT) : null;
	}

	public HttpServletRequest servletRequest() {
		return servletRequest;
	}

	public HttpServletResponse servletResponse() {
		return servletResponse;
	}

	public HttpSession session() {
		return session;
	}

	public JsonObject requestParts() {
		return requestParts;
	}

	/**
	 * The environment key under which an interceptor stored the request parts, or null if none did.
	 */
	public String requestId() {
		return requestId;
	}

	/**
	 * The endpoint the request resolved to; empty when {@link #unmatched()} is set.
	 */
	public Optional<EndpointRoute> route() {
		return Optional.ofNullable(route);
	}

	/**
	 * Why the request resolved to no endpoint, or null when it did resolve.
	 */
	public Unmatched unmatched() {
		return unmatched;
	}

	public EndpointRequest withRequestId(String requestId) {
		return new EndpointRequest(channel, path, servletRequest, servletResponse, session, requestParts,
			requestId, route, unmatched);
	}

	EndpointRequest withResolution(EndpointRoute route, Unmatched unmatched) {
		return new EndpointRequest(channel, path, servletRequest, servletResponse, session, requestParts,
			requestId, route, unmatched);
	}
}
