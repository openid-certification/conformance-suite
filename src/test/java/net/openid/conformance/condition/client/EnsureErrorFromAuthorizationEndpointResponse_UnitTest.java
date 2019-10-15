package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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
public class EnsureErrorFromAuthorizationEndpointResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureErrorFromAuthorizationEndpointResponse cond;

	@Before
	public void setUp() throws Exception {
		cond = new EnsureErrorFromAuthorizationEndpointResponse();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		JsonObject authorizationEndpointResponse = new JsonObject();

		authorizationEndpointResponse.addProperty("error", "invalid_request");

		authorizationEndpointResponse.addProperty("error_description", "incorrect credentials");

		authorizationEndpointResponse.addProperty("error_uri", "http://anerror.com");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_hasNotErrorField() {

		JsonObject authorizationEndpointResponse = env.getObject("authorization_endpoint_response");

		authorizationEndpointResponse.remove("error");

		env.putObject("authorization_endpoint_response", authorizationEndpointResponse);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_success() {
		cond.execute(env);
	}

}
