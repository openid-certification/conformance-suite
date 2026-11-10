package net.openid.conformance.testmodule.endpoint;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EndpointDispatcher_UnitTest {

	public static class AuthorizationServerEndpoints {

		@Endpoint(path = "authorize")
		public Object authorize() {
			return "authorize";
		}

		@Post(value = "/token", channels = { Channel.HTTP, Channel.MTLS }, consumes = "application/x-www-form-urlencoded")
		public Object token(EndpointRequest request) {
			return "token:" + request.channel() + ":" + request.requestId();
		}

		@Get("jwks")
		public Object jwks() {
			return "jwks";
		}

		@Get(value = "userinfo", channels = Channel.MTLS)
		public Object userinfo() {
			return "userinfo";
		}

		@WellKnown("oauth-authorization-server")
		@Endpoint(path = ".well-known/openid-configuration")
		public Object discovery() {
			return "discovery";
		}

		@Post("fail")
		public Object fail() {
			throw new IllegalArgumentException("boom");
		}
	}

	public static class NegativeTestEndpoints extends AuthorizationServerEndpoints {

		@Override
		public Object jwks() {
			return "broken-jwks";
		}
	}

	public static class ParEndpoints {

		@Post(value = "par", channels = { Channel.HTTP, Channel.MTLS })
		public Object par() {
			return "par";
		}
	}

	public static class VciEndpoints {

		@WellKnown("openid-credential-issuer")
		public Object issuerMetadata() {
			return "issuer";
		}

		@WellKnown("openid-credential-issuer/tenant")
		public Object tenantMetadata() {
			return "tenant";
		}
	}

	private static JsonObject parts(String method) {
		JsonObject parts = new JsonObject();
		parts.addProperty("method", method);
		return parts;
	}

	private static EndpointDispatcher dispatcher(Object... endpoints) {
		return EndpointDispatcher.builder()
			.endpoints(endpoints)
			.onUnmatched(r -> "unmatched:" + r.unmatched())
			.build();
	}

	private static Object send(EndpointDispatcher dispatcher, Channel channel, String method, String path) {
		return dispatcher.dispatch(channel, path, null, null, null, parts(method));
	}

	@Test
	public void routesByChannelPathAndMethod() {
		EndpointDispatcher d = dispatcher(new AuthorizationServerEndpoints(), new ParEndpoints());

		assertEquals("authorize", send(d, Channel.HTTP, "GET", "authorize"));
		assertEquals("authorize", send(d, Channel.HTTP, "POST", "authorize"));
		assertEquals("token:HTTP:null", send(d, Channel.HTTP, "POST", "token"));
		assertEquals("token:MTLS:null", send(d, Channel.MTLS, "POST", "/token"));
		assertEquals("par", send(d, Channel.MTLS, "POST", "par"));
		assertEquals("discovery", send(d, Channel.HTTP, "GET", ".well-known/openid-configuration"));
	}

	@Test
	public void reportsWhyARequestIsUnmatched() {
		EndpointDispatcher d = dispatcher(new AuthorizationServerEndpoints());

		assertEquals("unmatched:NO_ROUTE", send(d, Channel.HTTP, "GET", "nope"));
		assertEquals("unmatched:METHOD_NOT_ALLOWED", send(d, Channel.HTTP, "GET", "token"));
		assertEquals("unmatched:CHANNEL_NOT_ALLOWED", send(d, Channel.HTTP, "GET", "userinfo"));
		assertEquals("unmatched:CHANNEL_NOT_ALLOWED", send(d, Channel.MTLS, "GET", "jwks"));
	}

	@Test
	public void wellKnownMatchesTheSuffixAndPathsBelowIt() {
		EndpointDispatcher d = dispatcher(new AuthorizationServerEndpoints(), new VciEndpoints());

		assertEquals("discovery", send(d, Channel.WELL_KNOWN, "GET", "/.well-known/oauth-authorization-server"));
		assertEquals("discovery", send(d, Channel.WELL_KNOWN, "GET", "/.well-known/oauth-authorization-server/test/a/b"));
		assertEquals("unmatched:NO_ROUTE", send(d, Channel.WELL_KNOWN, "GET", "/.well-known/oauth-authorization-serverX"));
		assertEquals("issuer", send(d, Channel.WELL_KNOWN, "GET", "/.well-known/openid-credential-issuer/other"));
		assertEquals("tenant", send(d, Channel.WELL_KNOWN, "GET", "/.well-known/openid-credential-issuer/tenant/x"));
	}

	@Test
	public void subclassOverrideIsInvokedWithoutRepeatingTheAnnotation() {
		EndpointDispatcher d = dispatcher(new NegativeTestEndpoints());

		assertEquals("broken-jwks", send(d, Channel.HTTP, "GET", "jwks"));
		assertEquals("authorize", send(d, Channel.HTTP, "GET", "authorize"));
	}

	@Test
	public void handlerExceptionsPropagateUnwrapped() {
		EndpointDispatcher d = dispatcher(new AuthorizationServerEndpoints());

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> send(d, Channel.HTTP, "POST", "fail"));
		assertEquals("boom", e.getMessage());
	}

	@Test
	public void interceptorsRunInOrderAroundMatchedAndUnmatchedRequests() {
		List<String> seen = new ArrayList<>();
		EndpointDispatcher d = EndpointDispatcher.builder()
			.endpoints(new AuthorizationServerEndpoints())
			.interceptor((request, chain) -> {
				seen.add("outer:" + request.route().map(EndpointRoute::name).orElse(String.valueOf(request.unmatched())));
				return chain.proceed(request.withRequestId("req-1"));
			})
			.interceptor((request, chain) -> {
				seen.add("inner:" + request.requestId());
				return chain.proceed(request);
			})
			.onUnmatched(r -> "unmatched:" + r.requestId())
			.build();

		assertEquals("token:HTTP:req-1", send(d, Channel.HTTP, "POST", "token"));
		assertEquals("unmatched:req-1", send(d, Channel.HTTP, "GET", "nope"));
		assertEquals(List.of(
			"outer:AuthorizationServerEndpoints.token", "inner:req-1",
			"outer:NO_ROUTE", "inner:req-1"), seen);
	}

	@Test
	public void interceptorCanShortCircuitTheHandler() {
		EndpointDispatcher d = EndpointDispatcher.builder()
			.endpoints(new AuthorizationServerEndpoints())
			.interceptor((request, chain) -> "refused")
			.onUnmatched(r -> "unmatched")
			.build();

		assertEquals("refused", send(d, Channel.HTTP, "GET", "authorize"));
	}

	@Test
	public void routeExposesConsumesForInterceptors() {
		EndpointRoute token = EndpointRegistry.scan(new AuthorizationServerEndpoints())
			.resolve(Channel.HTTP, "token", "POST").route();

		assertNotNull(token);
		assertEquals(List.of("application/x-www-form-urlencoded"), token.consumes());
	}

	@Test
	public void storeIncomingRequestInterceptorPutsThePartsInTheEnvironment() {
		Environment env = new Environment();
		EndpointDispatcher d = EndpointDispatcher.builder()
			.endpoints(new AuthorizationServerEndpoints())
			.interceptor(new StoreIncomingRequestInterceptor(env))
			.onUnmatched(r -> "unmatched")
			.build();

		JsonObject parts = parts("POST");
		String response = (String) d.dispatch(Channel.HTTP, "token", null, null, null, parts);

		String requestId = response.substring("token:HTTP:".length());
		assertTrue(requestId.startsWith("incoming_request_"));
		assertSame(parts, env.getObject(requestId));
	}

	@Test
	public void dispatcherNeedsAnUnmatchedHandler() {
		assertThrows(IllegalStateException.class,
			() -> EndpointDispatcher.builder().endpoints(new AuthorizationServerEndpoints()).build());
	}
}
