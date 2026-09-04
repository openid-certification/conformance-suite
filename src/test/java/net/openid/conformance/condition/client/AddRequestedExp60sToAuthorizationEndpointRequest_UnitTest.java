package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class AddRequestedExp60sToAuthorizationEndpointRequest_UnitTest {

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	@Test
	public void addsSixtySecondRequestedExpiry() {
		Environment env = new Environment();
		env.putObject("authorization_endpoint_request", new JsonObject());
		AddRequestedExp60sToAuthorizationEndpointRequest condition =
			new AddRequestedExp60sToAuthorizationEndpointRequest();
		condition.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		condition.execute(env);

		assertThat(env.getInteger("authorization_endpoint_request", "requested_expiry"))
			.isEqualTo(60);
	}
}
