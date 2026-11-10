package net.openid.conformance.testmodule.endpoint;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EndpointRegistry_UnitTest {

	public static class Token {
		@Post("token")
		public Object token() {
			return "a";
		}
	}

	public static class AnotherToken {
		@Endpoint(path = "/token")
		public Object token() {
			return "b";
		}
	}

	public static class TokenOverMtls {
		@Post(value = "token", channels = Channel.MTLS)
		public Object token() {
			return "c";
		}
	}

	public static class GetToken {
		@Get("token")
		public Object token() {
			return "d";
		}
	}

	public static class StaticHandler {
		@Get("x")
		public static Object x() {
			return "x";
		}
	}

	public static class TwoParameters {
		@Get("x")
		public Object x(EndpointRequest request, String other) {
			return "x";
		}
	}

	public static class VoidHandler {
		@Get("x")
		public void x() {
		}
	}

	public static class WellKnownViaEndpoint {
		@Endpoint(path = ".well-known/x", channels = Channel.WELL_KNOWN)
		public Object x() {
			return "x";
		}
	}

	public static class NoChannels {
		@Endpoint(path = "x", channels = {})
		public Object x() {
			return "x";
		}
	}

	static class PackagePrivateClass {
		@Get("x")
		public Object x() {
			return "x";
		}
	}

	public static class PackagePrivateMethod {
		@Get("x")
		Object x() {
			return "x";
		}
	}

	@Test
	public void sameChannelPathAndMethodTwiceIsRejected() {
		IllegalStateException e = assertThrows(IllegalStateException.class,
			() -> EndpointRegistry.scan(new Token(), new AnotherToken()));
		assertTrue(e.getMessage().contains("Token.token"), e.getMessage());
	}

	@Test
	public void sameTokenPathOnOtherChannelOrMethodIsAllowed() {
		EndpointRegistry registry = EndpointRegistry.scan(new Token(), new TokenOverMtls(), new GetToken());
		assertEquals(3, registry.routes().size());
		assertEquals(Set.of("POST"), registry.resolve(Channel.HTTP, "token", "POST").route().methods());
		assertEquals(Set.of("GET"), registry.resolve(Channel.HTTP, "token", "GET").route().methods());
	}

	@Test
	public void invalidHandlersAreRejected() {
		assertThrows(IllegalStateException.class, () -> EndpointRegistry.scan(new StaticHandler()));
		assertThrows(IllegalStateException.class, () -> EndpointRegistry.scan(new TwoParameters()));
		assertThrows(IllegalStateException.class, () -> EndpointRegistry.scan(new VoidHandler()));
		assertThrows(IllegalStateException.class, () -> EndpointRegistry.scan(new WellKnownViaEndpoint()));
		assertThrows(IllegalStateException.class, () -> EndpointRegistry.scan(new NoChannels()));
		assertThrows(IllegalStateException.class, () -> EndpointRegistry.scan(new PackagePrivateClass()));
		assertThrows(IllegalStateException.class, () -> EndpointRegistry.scan(new PackagePrivateMethod()));
	}
}
