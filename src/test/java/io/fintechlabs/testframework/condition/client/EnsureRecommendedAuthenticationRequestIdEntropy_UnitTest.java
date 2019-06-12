package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
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
public class EnsureRecommendedAuthenticationRequestIdEntropy_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureRecommendedAuthenticationRequestIdEntropy cond;

	@Before
	public void setUp() throws Exception {
		cond = new EnsureRecommendedAuthenticationRequestIdEntropy();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_entropyGood() {
		JsonObject o = new JsonObject();
		o.addProperty("auth_req_id", "VggF4rKbpuQyjEV3MxVNOy_f-vRyWiNZbuHssshH8WY");
		env.putObject("backchannel_authentication_endpoint_response", o);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_entropyBad() {
		JsonObject o = new JsonObject();
		o.addProperty("auth_req_id", "1111111111111111111111111111111111111111");
		env.putObject("backchannel_authentication_endpoint_response", o);

		cond.evaluate(env);
	}

}
