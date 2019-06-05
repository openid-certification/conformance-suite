package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class EnsureResponseTypeIsCodeIdToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject authorizationEndpointRequest;

	private JsonObject invalidAuthorizationEndpointRequest;

	private EnsureResponseTypeIsCodeIdToken cond;

	@Before
	public void setUp() throws Exception {

		cond = new EnsureResponseTypeIsCodeIdToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		String token =  "code id_token";
		String invalidToken = "invalidToken";

		JsonObject sampleParams = new JsonObject();
		sampleParams.addProperty("response_type", token);

		JsonObject invalidParams = new JsonObject();
		invalidParams.addProperty("response_type", invalidToken);

		authorizationEndpointRequest = new JsonObject();
		authorizationEndpointRequest.add("params", sampleParams);

		invalidAuthorizationEndpointRequest = new JsonObject();
		invalidAuthorizationEndpointRequest.add("params", invalidParams);
	}

	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("authorization_endpoint_request", authorizationEndpointRequest);

		cond.evaluate(env);

		assertNotNull(env.getElementFromObject("authorization_endpoint_request", "params.response_type"));
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMismatch() {

		env.putObject("authorization_endpoint_request", invalidAuthorizationEndpointRequest);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		cond.evaluate(env);

	}

}
