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
public class ValidateAuthenticationRequestIdExpiresIn_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateAuthenticationRequestIdExpiresIn cond;

	@Before
	public void setUp() throws Exception {
		cond = new ValidateAuthenticationRequestIdExpiresIn();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_isGood() {
		JsonObject o = new JsonObject();
		o.addProperty("expires_in", 600);
		env.putObject("backchannel_authentication_endpoint_response", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_isNotNumber() {
		JsonObject o = new JsonObject();
		o.addProperty("expires_in", "600");
		env.putObject("backchannel_authentication_endpoint_response", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_lessthanZero() {
		JsonObject o = new JsonObject();
		o.addProperty("expires_in", -2);
		env.putObject("backchannel_authentication_endpoint_response", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_morethanOneYear() {
		JsonObject o = new JsonObject();
		o.addProperty("expires_in", 60000000);
		env.putObject("backchannel_authentication_endpoint_response", o);

		cond.execute(env);
	}
}
