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
public class EnsureErrorTokenEndpointSlowdownOrAuthorizationPending_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureErrorTokenEndpointSlowdownOrAuthorizationPending cond;

	@Before
	public void setUp() throws Exception {
		cond = new EnsureErrorTokenEndpointSlowdownOrAuthorizationPending();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvalutate_isGood() {
		JsonObject o = new JsonObject();

		// Case slow_down
		o.addProperty("error", "slow_down");
		env.putObject("token_endpoint_response", o);

		cond.execute(env);

		// Case authorization_pending
		o.addProperty("error", "authorization_pending");
		env.putObject("token_endpoint_response", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvalutate_isEmpty() {
		JsonObject o = new JsonObject();
		env.putObject("token_endpoint_response", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvalutate_isBad() {
		JsonObject o = new JsonObject();

		// Case access_denied
		o.addProperty("error", "access_denied");
		env.putObject("token_endpoint_response", o);

		cond.execute(env);

		// Case expired_token
		o.addProperty("error", "expired_token");
		env.putObject("token_endpoint_response", o);

		cond.execute(env);

		// Case unauthorized_client
		o.addProperty("error", "unauthorized_client");
		env.putObject("token_endpoint_response", o);

		cond.execute(env);
	}
}
