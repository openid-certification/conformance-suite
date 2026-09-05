package net.openid.conformance.fapiciba;

import com.google.gson.JsonObject;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToBackchannelRequest;
import net.openid.conformance.sequence.client.AddMTLSClientAuthenticationToRequest;
import net.openid.conformance.sequence.client.AddPrivateKeyJWTClientAuthenticationToBackchannelRequest;
import net.openid.conformance.sequence.client.CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AbstractFAPICIBAID1_UnitTest {

	@Test
	public void authorizationFlowCanFinishAfterHandlingAnErrorResponse() {
		TestableModule module = new TestableModule();

		module.performAuthorizationFlow();

		assertThat(module.events).containsExactly(
			"pre", "create", "request", "handle-error", "finished");
	}

	@Test
	public void usesAuthenticationMethodReturnedByBrazilDynamicRegistration() {
		TestableModule module = new TestableModule();
		module.useProfile(new OpenBankingBrazilCibaServerProfileBehavior());
		module.setupPrivateKeyJwt();
		module.setClientAuthenticationMethod("self_signed_tls_client_auth");

		assertThat(module.resolvedBackchannelClientAuthentication())
			.isInstanceOf(AddMTLSClientAuthenticationToBackchannelRequest.class);
		assertThat(module.resolvedTokenEndpointClientAuthentication())
			.isEqualTo(AddMTLSClientAuthenticationToRequest.class);

		module.setClientAuthenticationMethod("private_key_jwt");

		assertThat(module.resolvedBackchannelClientAuthentication())
			.isInstanceOf(AddPrivateKeyJWTClientAuthenticationToBackchannelRequest.class);
		assertThat(module.resolvedTokenEndpointClientAuthentication())
			.isEqualTo(CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class);

		module.useProfile(new FAPICIBAServerProfileBehavior());
		module.setClientAuthenticationMethod("self_signed_tls_client_auth");

		assertThat(module.resolvedBackchannelClientAuthentication())
			.isInstanceOf(AddPrivateKeyJWTClientAuthenticationToBackchannelRequest.class);
		assertThat(module.resolvedTokenEndpointClientAuthentication())
			.isEqualTo(CreateJWTClientAuthenticationAssertionAndAddToTokenEndpointRequest.class);
	}

	@Test
	public void routesBrazilNotificationOnlyOnMtlsEndpoint() {
		TestableModule module = new TestableModule();
		module.useProfile(new OpenBankingBrazilCibaServerProfileBehavior());
		JsonObject headers = new JsonObject();
		headers.addProperty("x-ssl-cert", "certificate");
		JsonObject request = new JsonObject();
		request.add("headers", headers);

		assertThat(module.handleHttp("ciba-notification-endpoint", null, null, null, new JsonObject()))
			.isEqualTo("unexpected");
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
		}

		private void useProfile(FAPICIBAServerProfileBehavior profileBehavior) {
			this.profileBehavior = profileBehavior;
		}

		private void setClientAuthenticationMethod(String method) {
			JsonObject client = new JsonObject();
			client.addProperty("token_endpoint_auth_method", method);
			env.putObject("client", client);
		}

		private ConditionSequence resolvedBackchannelClientAuthentication() {
			return getBackchannelClientAuthentication();
		}

		private Class<? extends ConditionSequence> resolvedTokenEndpointClientAuthentication() {
			return getTokenEndpointClientAuthentication();
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
