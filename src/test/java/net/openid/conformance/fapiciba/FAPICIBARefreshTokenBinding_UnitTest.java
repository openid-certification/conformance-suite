package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToRequest;
import net.openid.conformance.testmodule.TestModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

// The paired CIBA RP finishes after its first resource request, before the two-client binding check.
// Start after client 2's successful refresh and exercise the negative phase with real HTTP.
public class FAPICIBARefreshTokenBinding_UnitTest {

	private HttpServer server;
	private RefreshTokenModule module;
	private volatile String requestBody;
	private volatile boolean returnInvalidScope;

	private static class RefreshTokenModule extends FAPICIBAID1RefreshToken {
		private boolean finished;

		@Override
		public String getName() {
			return "refresh-token-binding-test";
		}

		void initialize(String tokenEndpoint) {
			setProperties("UNIT-TEST", Map.of(), BsonEncoding.testInstanceEventLog(), null,
				mock(TestInfoService.class), null, null);
			addTokenEndpointClientAuthentication = AddMTLSClientAuthenticationToRequest.class;
			JsonObject serverConfiguration = new JsonObject();
			serverConfiguration.addProperty("token_endpoint", tokenEndpoint);
			env.putObject("server", serverConfiguration);
			env.putObject("client", client("client1", "openid consent:client1"));
			env.putObject("client2", client("client2", "openid consent:client2"));
			env.putString("refresh_token", "client2-refresh-token");
			switchToSecondClient();
			setStatus(Status.CONFIGURED);
			setStatus(Status.RUNNING);
		}

		private JsonObject client(String clientId, String scope) {
			JsonObject client = new JsonObject();
			client.addProperty("client_id", clientId);
			client.addProperty("scope", scope);
			return client;
		}

		@Override
		protected void sendRefreshTokenRequestAndCheckIdTokenClaims() {
		}

		@Override
		protected void requestProtectedResource() {
		}

		@Override
		public void fireTestFinished() {
			finished = true;
			setStatus(Status.WAITING);
		}

		void releaseTestLock() {
			clearLockIfHeld();
		}
	}

	@BeforeEach
	public void startTokenEndpoint() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/token", exchange -> {
			try (exchange) {
				requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
				String error = returnInvalidScope || requestBody.contains("scope=") ? "invalid_scope" : "invalid_grant";
				byte[] response = ("{\"error\":\"" + error + "\"}").getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "application/json");
				exchange.sendResponseHeaders(400, response.length);
				exchange.getResponseBody().write(response);
			}
		});
		server.start();
		module = new RefreshTokenModule();
		module.initialize("http://127.0.0.1:" + server.getAddress().getPort() + "/token");
	}

	@AfterEach
	public void stopTokenEndpoint() {
		if (module != null) {
			module.releaseTestLock();
		}
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	public void wrongClientRefreshOmitsScopeAndAcceptsInvalidGrant() {
		module.performPostAuthorizationFlow(true);

		assertThat(requestBody).contains("grant_type=refresh_token", "refresh_token=client2-refresh-token", "client_id=client1")
			.doesNotContain("scope=");
		assertThat(module.getResult()).isEqualTo(TestModule.Result.UNKNOWN);
		assertThat(module.finished).isTrue();
	}

	@Test
	public void wrongClientRefreshStillFailsOnInvalidScope() {
		returnInvalidScope = true;

		module.performPostAuthorizationFlow(true);

		assertThat(module.getResult()).isEqualTo(TestModule.Result.FAILED);
		assertThat(module.finished).isTrue();
	}
}
