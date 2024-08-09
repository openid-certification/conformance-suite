package net.openid.conformance.condition.client;

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
public class CheckErrorFromBackchannelAuthenticationEndpointError_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckErrorFromBackchannelAuthenticationEndpointError cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckErrorFromBackchannelAuthenticationEndpointError();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseInvalidRequest() {
		env.putObject("backchannel_authentication_endpoint_response", JsonParser.parseString("{\"error\":\"invalid_request\"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseAccessDenied() {
		env.putObject("backchannel_authentication_endpoint_response", JsonParser.parseString("{\"error\":\"access_denied\"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseInvalidClient() {
		env.putObject("backchannel_authentication_endpoint_response", JsonParser.parseString("{\"error\":\"invalid_client\"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseInvalidRequestObject() {
		assertThrows(ConditionError.class, () -> {
			env.putObject("backchannel_authentication_endpoint_response", JsonParser.parseString("{\"error\":\"invalid_request_object\"}").getAsJsonObject());

			cond.execute(env);
		});
	}
}
