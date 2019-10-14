package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
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
public class VerifyNoStateInAuthorizationResponse_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private VerifyNoStateInAuthorizationResponse cond;

	private JsonObject responseWithState;

	private JsonObject responseWithoutState;

	@Before
	public void setUp() throws Exception {

		cond = new VerifyNoStateInAuthorizationResponse();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		responseWithState = new JsonParser().parse("{"
			+ "\"code\":\"SplxlOBeZQQYbYS6WxSbIA\","
			+ "\"state\":\"xyz\""
			+ "}").getAsJsonObject();

		responseWithoutState = new JsonParser().parse("{"
			+ "\"code\":\"SplxlOBeZQQYbYS6WxSbIA\""
			+ "}").getAsJsonObject();
	}

	@Test
	public void testEvaluate_noError() {
		env.putObject("authorization_endpoint_response", responseWithoutState);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_presentState() {
		env.putObject("authorization_endpoint_response", responseWithState);
		cond.execute(env);
	}

}
