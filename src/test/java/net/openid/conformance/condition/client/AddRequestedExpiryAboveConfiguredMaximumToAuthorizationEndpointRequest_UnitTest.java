package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@Test
	public void addsValueOneSecondAboveDefaultMaximum() {
		Environment env = new Environment();
		env.putObject("authorization_endpoint_request", new JsonObject());
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
		JsonObject client = new JsonObject();
		client.addProperty("brazil_ciba_maximum_expiry", "3600");
		env.putObject("client", client);
		AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest condition =
			new AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		condition.execute(env);

		assertThat(env.getInteger("authorization_endpoint_request", "requested_expiry"))
			.isEqualTo(3_601);
	}
}
