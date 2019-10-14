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

@RunWith(MockitoJUnitRunner.class)
public class EnsureMinimumRefreshTokenLength_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureMinimumRefreshTokenLength cond;

	@Before
	public void setUp() throws Exception {
		cond = new EnsureMinimumRefreshTokenLength();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_entropyGood() {
		JsonObject o = new JsonObject();
		o.addProperty("refresh_token", "HEI92Ptt0wGHDCeJKKv4hpnNRljn3it-KyvX25Tffa4");
		env.putObject("token_endpoint_response", o);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_entropyBad() {
		JsonObject o = new JsonObject();
		o.addProperty("refresh_token", "HEI92Ptt0w");
		env.putObject("token_endpoint_response", o);

		cond.execute(env);
	}

}
