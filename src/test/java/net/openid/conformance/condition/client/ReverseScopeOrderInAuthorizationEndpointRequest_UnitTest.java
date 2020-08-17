package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ReverseScopeOrderInAuthorizationEndpointRequest_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ReverseScopeOrderInAuthorizationEndpointRequest cond;

	private String requestId;

	@Before
	public void setUp() throws Exception {
		cond = new ReverseScopeOrderInAuthorizationEndpointRequest();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate() {
		JsonObject authRequest = new JsonObject();
		authRequest.addProperty("scope", "openid accounts");
		env.putObject("authorization_endpoint_request", authRequest);

		cond.execute(env);

		assertThat(env.getString("authorization_endpoint_request", "scope")).isEqualTo("accounts openid");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluateNoScope() {
		JsonObject authRequest = new JsonObject();
		env.putObject("authorization_endpoint_request", authRequest);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluateScopeEmpty() {
		JsonObject authRequest = new JsonObject();
		authRequest.addProperty("scope", "");
		env.putObject("authorization_endpoint_request", authRequest);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluateOnlyOneScope() {
		JsonObject authRequest = new JsonObject();
		authRequest.addProperty("scope", "openid");
		env.putObject("authorization_endpoint_request", authRequest);

		cond.execute(env);
	}

}
