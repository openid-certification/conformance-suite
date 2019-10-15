package net.openid.conformance.condition.client;

import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest cond;

	@Before
	public void setUp() throws Exception {
		cond = new CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseInvalidRequest() {
		env.putObject("token_endpoint_response", new JsonParser().parse("{\"error\":\"invalid_request\"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test
	public void testEvaluate_caseInvalidClient() {
		env.putObject("token_endpoint_response", new JsonParser().parse("{\"error\":\"invalid_client\"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseAccessDenied() {
		env.putObject("token_endpoint_response", new JsonParser().parse("{\"error\":\"access_denied\"}").getAsJsonObject());

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseInvalidRequestObject() {
		env.putObject("token_endpoint_response", new JsonParser().parse("{\"error\":\"invalid_request_object\"}").getAsJsonObject());

		cond.execute(env);
	}

}
