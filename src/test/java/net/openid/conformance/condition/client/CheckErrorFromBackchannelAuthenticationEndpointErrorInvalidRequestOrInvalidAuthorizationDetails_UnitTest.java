package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequestOrInvalidAuthorizationDetails_UnitTest {

	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequestOrInvalidAuthorizationDetails cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequestOrInvalidAuthorizationDetails();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseInvalidRequest() {
		JsonObject response = JsonParser.parseString("{\"error\":\"invalid_request\"}").getAsJsonObject();

		env.putObject("backchannel_authentication_endpoint_response", response);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseInvalidAuthorizationDetails() {
		JsonObject response = JsonParser.parseString("{\"error\":\"invalid_authorization_details\"}").getAsJsonObject();

		env.putObject("backchannel_authentication_endpoint_response", response);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseWrongError() {
		assertThrows(ConditionError.class, () -> {
			JsonObject response = JsonParser.parseString("{\"error\":\"invalid_client\"}").getAsJsonObject();

			env.putObject("backchannel_authentication_endpoint_response", response);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_caseErrorEmpty() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("backchannel_authentication_endpoint_response", new JsonObject());

			cond.execute(env);
		});
	}
}
