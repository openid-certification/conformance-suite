package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.GeneratePS256ClientJWKsWithKeyID;
import net.openid.conformance.info.TestInfoService;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.runner.TestExecutionManager;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AbstractFAPICIBAID1_UnitTest {

	@Test
	public void authorizationFlowCanFinishAfterHandlingAnErrorResponse() {
		TestableModule module = new TestableModule();

		module.performAuthorizationFlow();

		assertThat(module.events).containsExactly(
			"pre", "create", "request", "handle-error", "finished");
	}

	@ParameterizedTest
	@ValueSource(strings = { "private_key_jwt", "tls_client_auth", "self_signed_tls_client_auth" })
	public void keepsSelectedPrivateKeyJwtAuthentication(String registeredMethod) {
		AbstractFAPICIBAID1 module = new FAPICIBAID1EnsureOtherScopeOrderSucceeds();
		module.setProperties("UNIT-TEST", Map.of("sub", "unit-test", "iss", "https://issuer.example"), BsonEncoding.testInstanceEventLog(), null,
			mock(TestInfoService.class), null, null);
		module.setupOpenBankingBrazil();
		module.setupPrivateKeyJwt();
		Environment env = module.getEnv();
		env.putObjectFromJsonString("client", "{\"client_id\":\"registered-client\"}");
		env.putString("client", "token_endpoint_auth_method", registeredMethod);
		env.putObjectFromJsonString("server", """
			{
			  "token_endpoint": "https://server.example/token",
			  "backchannel_authentication_endpoint": "https://server.example/backchannel"
			}
			""");
		GeneratePS256ClientJWKsWithKeyID generateKeys = new GeneratePS256ClientJWKsWithKeyID();
		generateKeys.setProperties("UNIT-TEST", BsonEncoding.testInstanceEventLog(), ConditionResult.FAILURE);
		generateKeys.execute(env);
		env.putObject("backchannel_authentication_endpoint_request_form_parameters", new JsonObject());
		env.putObject("token_endpoint_request_form_parameters", new JsonObject());

		module.addClientAuthenticationToBackchannelRequest();
		module.addClientAuthenticationToTokenEndpointRequest();

		for (String request : List.of("backchannel_authentication_endpoint_request_form_parameters",
			"token_endpoint_request_form_parameters")) {
			assertThat(env.getString(request, "client_assertion")).isNotEmpty();
			assertThat(env.getString(request, "client_assertion_type"))
				.isEqualTo("urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
		}
	}

	@Test
	public void routesBrazilNotificationOnlyOnMtlsEndpoint() {
		TestableModule module = new TestableModule();
		module.useProfile(new OpenBankingBrazilCibaServerProfileBehavior());
		JsonObject headers = new JsonObject();
		headers.addProperty("x-ssl-cert", "certificate");
		JsonObject request = new JsonObject();
		request.add("headers", headers);

		ResponseEntity<?> wrongHost = (ResponseEntity<?>) module.handleHttp(
			"ciba-notification-endpoint", null, null, null, new JsonObject());
		assertThat(wrongHost.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(module.handleHttpMtls("ciba-notification-endpoint", null, null, null, request))
			.isEqualTo("ping");
	}

	@Test
	public void keepsGenericNotificationOnRegularEndpoint() {
		TestableModule module = new TestableModule();
		module.useProfile(new FAPICIBAServerProfileBehavior());

		assertThat(module.handleHttp("ciba-notification-endpoint", null, null, null, new JsonObject()))
			.isEqualTo("ping");
		assertThat(module.handleHttpMtls("ciba-notification-endpoint", null, null, null, new JsonObject()))
			.isEqualTo("unexpected");
	}

	private static class TestableModule extends AbstractFAPICIBAID1 {

		private final List<String> events = new ArrayList<>();

		TestableModule() {
			eventLog = BsonEncoding.testInstanceEventLog();
			executionManager = mock(TestExecutionManager.class);
		}

		private void useProfile(FAPICIBAServerProfileBehavior profileBehavior) {
			this.profileBehavior = profileBehavior;
		}

		@Override
		protected Object handlePingCallback(JsonObject requestParts) {
			return "ping";
		}

		@Override
		protected Object unexpectedHttpRequest(String path, JsonObject requestParts) {
			return "unexpected";
		}

		@Override
		protected void performPreAuthorizationSteps() {
			events.add("pre");
		}

		@Override
		protected void createAuthorizationRequest() {
			events.add("create");
		}

		@Override
		protected void performAuthorizationRequest() {
			events.add("request");
		}

		@Override
		protected boolean handleAuthorizationEndpointErrorResponse() {
			events.add("handle-error");
			return true;
		}

		@Override
		protected void performValidateAuthorizationResponse() {
			events.add("validate-success");
		}

		@Override
		protected void performPostAuthorizationResponse() {
			events.add("post");
		}

		@Override
		public void fireTestFinished() {
			events.add("finished");
		}
	}
}
