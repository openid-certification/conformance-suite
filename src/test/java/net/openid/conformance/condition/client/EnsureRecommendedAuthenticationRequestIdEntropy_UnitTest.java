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

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_entropyBad() {
		JsonObject o = new JsonObject();
		o.addProperty("auth_req_id", "1111111111111111111111111111111111111111");
		env.putObject("backchannel_authentication_endpoint_response", o);

		cond.execute(env);
	}

}
