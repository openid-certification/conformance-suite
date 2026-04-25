package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckBackchannelExpiresInDoesNotMatchRequestedExpiry_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckBackchannelExpiresInDoesNotMatchRequestedExpiry cond;

	@BeforeEach
	public void setUp() {
		cond = new CheckBackchannelExpiresInDoesNotMatchRequestedExpiry();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_succeedsWhenExpiresInDifferentFromRequestedExpiry() {
		JsonObject authorizationRequest = new JsonObject();
		authorizationRequest.addProperty("requested_expiry", 13);
		env.putObject("authorization_endpoint_request", authorizationRequest);

		JsonObject backchannelResponse = new JsonObject();
		backchannelResponse.addProperty("expires_in", 300);
		env.putObject("backchannel_authentication_endpoint_response", backchannelResponse);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_failsWhenExpiresInMatchesRequestedExpiry() {
		assertThrows(ConditionError.class, () -> {
			JsonObject authorizationRequest = new JsonObject();
			authorizationRequest.addProperty("requested_expiry", 13);
			env.putObject("authorization_endpoint_request", authorizationRequest);

			JsonObject backchannelResponse = new JsonObject();
			backchannelResponse.addProperty("expires_in", 13);
			env.putObject("backchannel_authentication_endpoint_response", backchannelResponse);

			cond.execute(env);
		});
	}
}
