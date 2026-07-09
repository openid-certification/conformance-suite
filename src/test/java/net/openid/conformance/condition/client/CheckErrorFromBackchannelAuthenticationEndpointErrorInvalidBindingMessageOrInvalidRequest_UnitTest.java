package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessageOrInvalidRequest_UnitTest {

	private Environment env;

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessageOrInvalidRequest cond;

	@BeforeEach
	public void setUp() {
		env = new Environment();
		cond = new CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidBindingMessageOrInvalidRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void succeedsForInvalidBindingMessage() {
		setError("invalid_binding_message");

		cond.execute(env);
	}

	@Test
	public void succeedsForInvalidRequest() {
		setError("invalid_request");

		cond.execute(env);
	}

	@Test
	public void throwsForWrongError() {
		setError("invalid_client");

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	@Test
	public void throwsWhenErrorIsMissing() {
		env.putObject("backchannel_authentication_endpoint_response", new JsonObject());

		assertThrows(ConditionError.class, () -> cond.execute(env));
	}

	private void setError(String error) {
		JsonObject response = new JsonObject();
		response.addProperty("error", error);
		env.putObject("backchannel_authentication_endpoint_response", response);
	}
}
