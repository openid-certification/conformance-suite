package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseGood() {
		JsonObject response = JsonParser.parseString("{\"error_description\":\"[A167303] A request object included in a backchannel authentication request must be signed.\",\"error\":\"invalid_request\",\"error_uri\":\"https://www.authlete.com/documents/apis/result_codes#A167303\"}").getAsJsonObject();

		env.putObject("backchannel_authentication_endpoint_response", response);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseWrongError() {
		assertThrows(ConditionError.class, () -> {
			JsonObject response = JsonParser.parseString("{\"error_description\":\"[A167303] A request object included in a backchannel authentication request must be signed.\",\"error\":\"invalid_client\",\"error_uri\":\"https://www.authlete.com/documents/apis/result_codes#A167303\"}").getAsJsonObject();

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
