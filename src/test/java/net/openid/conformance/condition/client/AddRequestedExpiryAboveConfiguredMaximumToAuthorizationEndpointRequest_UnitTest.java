package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@Test
	public void missingConfigFailsPrecondition() {
		Environment env = new Environment();
		env.putObject("authorization_endpoint_request", new JsonObject());
		AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest condition =
			new AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		assertThatThrownBy(() -> condition.execute(env)).isInstanceOf(ConditionError.class);
	}

	@Test
	public void addsValueOneSecondAboveDefaultMaximum() {
		Environment env = new Environment();
		env.putObject("authorization_endpoint_request", new JsonObject());
		putConfiguredMaximum(env, null);
		AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest condition =
			new AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		condition.execute(env);

		assertThat(env.getInteger("authorization_endpoint_request", "requested_expiry"))
			.isEqualTo(86_401);
	}

	@Test
	public void addsValueOneSecondAboveConfiguredMaximum() {
		Environment env = new Environment();
		env.putObject("authorization_endpoint_request", new JsonObject());
		putConfiguredMaximum(env, 3_600);
		AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest condition =
			new AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		condition.execute(env);

		assertThat(env.getInteger("authorization_endpoint_request", "requested_expiry"))
			.isEqualTo(3_601);
	}

	@Test
	public void usesConfiguredMaximumAfterDynamicRegistrationReplacesActiveClient() {
		Environment env = new Environment();
		env.putObject("authorization_endpoint_request", new JsonObject());
		putConfiguredMaximum(env, 60);
		env.putObjectFromJsonString("dynamic_registration_endpoint_response", """
			{
				"body_json": {
					"client_id": "dynamically-registered-client"
				}
			}
			""");
		ExtractDynamicRegistrationResponse extractDynamicRegistrationResponse =
			new ExtractDynamicRegistrationResponse();
		extractDynamicRegistrationResponse.setProperties(
			"UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
		extractDynamicRegistrationResponse.execute(env);
		AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest condition =
			new AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		condition.execute(env);

		assertThat(env.getInteger("authorization_endpoint_request", "requested_expiry"))
			.isEqualTo(61);
	}

	private void putConfiguredMaximum(Environment env, Integer seconds) {
		JsonObject client = new JsonObject();
		if (seconds != null) {
			client.addProperty("brazil_ciba_maximum_expiry", seconds);
		}
		JsonObject config = new JsonObject();
		config.add("client", client);
		env.putObject("config", config);
	}
}
