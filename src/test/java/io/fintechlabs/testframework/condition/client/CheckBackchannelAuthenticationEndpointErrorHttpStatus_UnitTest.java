package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CheckBackchannelAuthenticationEndpointErrorHttpStatus_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private CheckBackchannelAuthenticationEndpointErrorHttpStatus cond;

	@Before
	public void setUp() throws Exception {
		cond = new CheckBackchannelAuthenticationEndpointErrorHttpStatus();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_caseInvalidRequest() {
		env.putInteger("backchannel_authentication_endpoint_response_http_status", 400);
		env.putObject("backchannel_authentication_endpoint_response", new JsonParser().parse("{\"error\":\"invalid_request\"}").getAsJsonObject());

		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_caseAccessDenied() {
		env.putInteger("backchannel_authentication_endpoint_response_http_status", 403);
		env.putObject("backchannel_authentication_endpoint_response", new JsonParser().parse("{\"error\":\"access_denied\"}").getAsJsonObject());

		cond.evaluate(env);
	}

	@Test
	public void testEvaluate_caseInvalidClient() {
		env.putInteger("backchannel_authentication_endpoint_response_http_status", 401);
		env.putObject("backchannel_authentication_endpoint_response", new JsonParser().parse("{\"error\":\"invalid_client\"}").getAsJsonObject());

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseInvalidRequestObject() {
		env.putInteger("backchannel_authentication_endpoint_response_http_status", 400);
		env.putObject("backchannel_authentication_endpoint_response", new JsonParser().parse("{\"error\":\"invalid_request_object\"}").getAsJsonObject());

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseBadHttpStatusInvalidClient() {
		env.putInteger("backchannel_authentication_endpoint_response_http_status", 400);
		env.putObject("backchannel_authentication_endpoint_response", new JsonParser().parse("{\"error\":\"invalid_client\"}").getAsJsonObject());

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseBadHttpStatusInvalidRequest() {
		env.putInteger("backchannel_authentication_endpoint_response_http_status", 401);
		env.putObject("backchannel_authentication_endpoint_response", new JsonParser().parse("{\"error\":\"invalid_request\"}").getAsJsonObject());

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseBadHttpStatusInvalidRequestObject() {
		env.putInteger("backchannel_authentication_endpoint_response_http_status", 401);
		env.putObject("backchannel_authentication_endpoint_response", new JsonParser().parse("{\"error\":\"invalid_request_object\"}").getAsJsonObject());

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_caseBadHttpStatusAccessDenied() {
		env.putInteger("backchannel_authentication_endpoint_response_http_status", 401);
		env.putObject("backchannel_authentication_endpoint_response", new JsonParser().parse("{\"error\":\"access_denied\"}").getAsJsonObject());

		cond.evaluate(env);
	}
}
