package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpServer;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.condition.client.WaitForBrazilResourcesResponse;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.testmodule.TestFailureException;
import net.openid.conformance.testmodule.TestModule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

// The paired CIBA RP finishes after its first resource request and only returns 200.
// Use real HTTP responses to exercise the two-client module's resource and Accept-header paths.
public class FAPICIBAResourceResponse_UnitTest {

	private HttpServer server;
	private ResourceModule module;
	private int responseStatus = 200;
	private String responseBody = "{}";
	private String contentType = "application/json";
	private int pendingResponses;
	private int resourceCalls;

	private static class ResourceModule extends FAPICIBAID1 {
		private final List<Long> waits = new ArrayList<>();

		@Override
		protected WaitForBrazilResourcesResponse createResourceEndpointPollingWait(long deadline, int attempt) {
			return new NoSleepWaitForBrazilResourcesResponse(deadline, attempt);
		}

		private class NoSleepWaitForBrazilResourcesResponse extends WaitForBrazilResourcesResponse {
			NoSleepWaitForBrazilResourcesResponse(long deadline, int attempt) {
				super(deadline, attempt);
			}

			@Override
			protected void sleepForSeconds(long seconds) {
				waits.add(seconds);
			}
		}

		@Override
		public String getName() {
			return "resource-response-test";
		}

		void initialize(FAPICIBAServerProfileBehavior behavior, String resourceUrl) {
			setProperties("UNIT-TEST", Map.of(), BsonEncoding.testInstanceEventLog(), null,
				mock(TestInfoService.class), null, null);
			profileBehavior = behavior;
			behavior.setModule(this);
			JsonObject token = new JsonObject();
			token.addProperty("value", "test-access-token");
			token.addProperty("type", "Bearer");
			env.putString("consent_id", "test-consent");
			env.putObject("access_token", token);
			env.putObject("resource", new JsonObject());
			env.putString("protected_resource_url", resourceUrl);
			setStatus(Status.CONFIGURED);
			setStatus(Status.RUNNING);
			accessTokenReceived();
		}

		void accessTokenReceived() {
			call(profileBehavior.onSuccessfulTokenEndpointResponse());
		}

		void releaseTestLock() {
			clearLockIfHeld();
		}
	}

	@BeforeEach
	public void startResourceServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			try (exchange) {
				if (exchange.getRequestURI().getQuery() != null) {
					exchange.sendResponseHeaders(401, -1);
					return;
				}
				resourceCalls++;
				if (!"Bearer test-access-token".equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
					exchange.sendResponseHeaders(401, -1);
					return;
				}
				exchange.getResponseHeaders().set("x-fapi-interaction-id",
					exchange.getRequestHeaders().getFirst("x-fapi-interaction-id"));
				if (pendingResponses > 0) {
					pendingResponses--;
					exchange.sendResponseHeaders(202, -1);
					return;
				}
				if (contentType != null) {
					exchange.getResponseHeaders().set("Content-Type", contentType);
				}
				byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(responseStatus, body.length == 0 ? -1 : body.length);
				if (body.length != 0) {
					exchange.getResponseBody().write(body);
				}
			}
		});
		server.start();
	}

	@AfterEach
	public void stopResourceServer() {
		if (module != null) {
			module.releaseTestLock();
		}
		server.stop(0);
	}

	private void initialize(FAPICIBAServerProfileBehavior behavior) {
		module = new ResourceModule();
		module.initialize(behavior, "http://127.0.0.1:" + server.getAddress().getPort() + "/open-banking/resources/v3/resources");
	}

	@ParameterizedTest
	@ValueSource(ints = {3, 4, 10})
	public void brazilPollsUntil200BeforeBothAcceptHeaderProbes(int version) {
		pendingResponses = 2;
		initialize(new OpenBankingBrazilCibaServerProfileBehavior());
		module.getEnv().putString("protected_resource_url",
			"http://127.0.0.1:" + server.getAddress().getPort() + "/open-banking/resources/v" + version + "/resources");
		module.requestProtectedResource();
		module.verifyAccessTokenWithResourceEndpointDifferentAcceptHeader();
		assertThat(resourceCalls).isEqualTo(5);
		assertThat(module.waits).containsExactly(1L, 2L);
		assertThat(module.getResult()).isEqualTo(TestModule.Result.UNKNOWN);
		assertThat(module.getEnv().isKeyShadowed("endpoint_response")).isFalse();
	}

	@Test
	public void brazilStartsFreshPollingBudgetWhenAccessTokenArrives() {
		pendingResponses = 1;
		initialize(new OpenBankingBrazilCibaServerProfileBehavior());
		module.getEnv().putLong("brazil_resources_polling_started", System.nanoTime() - TimeUnit.MINUTES.toNanos(6));
		module.accessTokenReceived();
		module.requestProtectedResource();
		assertThat(module.getResult()).isEqualTo(TestModule.Result.UNKNOWN);
		assertThat(resourceCalls).isEqualTo(2);
		assertThat(module.waits).containsExactly(1L);
	}

	@Test
	public void brazilFailsOn202WhenTokenResponseUsedUpThePollingBudget() {
		responseStatus = 202;
		responseBody = "";
		initialize(new OpenBankingBrazilCibaServerProfileBehavior());
		module.getEnv().putLong("brazil_resources_polling_started", System.nanoTime() - TimeUnit.MINUTES.toNanos(5));
		var failure = assertThrows(TestFailureException.class, () -> module.requestProtectedResource());
		assertThat(failure).hasMessageContaining("WaitForBrazilResourcesResponse");
		assertThat(resourceCalls).isEqualTo(1);
	}

	@Test
	public void brazilRejectsNonEmpty202() {
		responseStatus = 202;
		responseBody = "{}";
		initialize(new OpenBankingBrazilCibaServerProfileBehavior());
		assertThrows(TestFailureException.class, () -> module.requestProtectedResource());
		assertThat(resourceCalls).isEqualTo(1);
	}

	@ParameterizedTest
	@ValueSource(ints = {200, 201})
	public void brazilStillRequiresJsonForNormalResponses(int status) {
		contentType = null;
		responseStatus = status;
		responseBody = "{}";
		initialize(new OpenBankingBrazilCibaServerProfileBehavior());
		module.requestProtectedResource();
		assertThat(module.getResult()).isEqualTo(TestModule.Result.FAILED);
	}

	@ParameterizedTest
	@ValueSource(ints = {200, 201})
	public void brazilAcceptsNormalJsonResponses(int status) {
		responseStatus = status;
		responseBody = "{}";
		contentType = "application/json";
		initialize(new OpenBankingBrazilCibaServerProfileBehavior());
		module.requestProtectedResource();
		assertThat(module.getResult()).isEqualTo(TestModule.Result.UNKNOWN);
	}

	@Test
	public void brazilAcceptHeaderProbesStillReject201() {
		responseStatus = 201;
		responseBody = "{}";
		contentType = "application/json";
		initialize(new OpenBankingBrazilCibaServerProfileBehavior());
		module.requestProtectedResource();
		module.verifyAccessTokenWithResourceEndpointDifferentAcceptHeader();
		assertThat(module.getResult()).isEqualTo(TestModule.Result.FAILED);
	}

	@Test
	public void brazilRejects202AfterCompletedResponse() {
		initialize(new OpenBankingBrazilCibaServerProfileBehavior());
		module.requestProtectedResource();
		responseStatus = 202;
		responseBody = "";
		assertThrows(TestFailureException.class, () -> module.requestProtectedResource());
		assertThat(resourceCalls).isEqualTo(2);
	}

	@ParameterizedTest
	@ValueSource(strings = {"/open-banking/accounts/v2/accounts", "/open-banking/resources/vnext/resources",
		"/open-banking/resources/v4beta/resources"})
	public void brazilRejects202OutsideNumericResourcesEndpoint(String path) {
		responseStatus = 202;
		responseBody = "";
		initialize(new OpenBankingBrazilCibaServerProfileBehavior());
		module.getEnv().putString("protected_resource_url",
			"http://127.0.0.1:" + server.getAddress().getPort() + path);
		assertThrows(TestFailureException.class, () -> module.requestProtectedResource());
		assertThat(resourceCalls).isEqualTo(1);
	}

	@Test
	public void brazilRejects202OnLaterAcceptHeaderProbes() {
		initialize(new OpenBankingBrazilCibaServerProfileBehavior());
		module.requestProtectedResource();
		responseStatus = 202;
		responseBody = "";
		module.verifyAccessTokenWithResourceEndpointDifferentAcceptHeader();
		assertThat(module.getResult()).isEqualTo(TestModule.Result.FAILED);
		assertThat(resourceCalls).isEqualTo(3);
	}

	@Test
	public void brazilStillRequiresJsonAfterPolling() {
		pendingResponses = 1;
		contentType = null;
		initialize(new OpenBankingBrazilCibaServerProfileBehavior());
		module.requestProtectedResource();
		assertThat(module.getResult()).isEqualTo(TestModule.Result.FAILED);
		assertThat(resourceCalls).isEqualTo(2);
	}

	@Test
	public void brazilStopsPollingOnServerError() {
		pendingResponses = 1;
		responseStatus = 504;
		initialize(new OpenBankingBrazilCibaServerProfileBehavior());
		assertThrows(TestFailureException.class, () -> module.requestProtectedResource());
		assertThat(resourceCalls).isEqualTo(2);
	}

	@Test
	public void brazilCanPollAgainForANewConsent() {
		pendingResponses = 1;
		initialize(new OpenBankingBrazilCibaServerProfileBehavior());
		module.requestProtectedResource();
		pendingResponses = 1;
		module.getEnv().putString("consent_id", "second-consent");
		module.requestProtectedResource();
		assertThat(module.getResult()).isEqualTo(TestModule.Result.UNKNOWN);
		assertThat(resourceCalls).isEqualTo(4);
	}

	@Test
	public void plainFapiStillRejects202() {
		responseStatus = 202;
		responseBody = "";
		initialize(new FAPICIBAServerProfileBehavior());
		var failure = assertThrows(TestFailureException.class, () -> module.requestProtectedResource());
		assertThat(failure).hasMessageContaining("EnsureHttpStatusCodeIs200or201");
	}
}
