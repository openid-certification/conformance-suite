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
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EnsureMinimumAuthenticationRequestIdLength_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureMinimumAuthenticationRequestIdLength cond;

	@Before
	public void setUp() throws Exception {
		cond = new EnsureMinimumAuthenticationRequestIdLength();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_lengthGood() {
		JsonObject o = new JsonObject();
		o.addProperty("auth_req_id", "VggF4rKbpuQyjEV3MxVNOy_f-vRyWiNZbuHssshH8WY");
		env.putObject("backchannel_authentication_endpoint_response", o);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_lengthBad() {
		JsonObject o = new JsonObject();
		o.addProperty("auth_req_id", "1c266114");
		env.putObject("backchannel_authentication_endpoint_response", o);

		cond.evaluate(env);
	}

}
