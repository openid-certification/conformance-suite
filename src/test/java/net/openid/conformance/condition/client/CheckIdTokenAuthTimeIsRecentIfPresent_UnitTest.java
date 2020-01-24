package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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

import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public class CheckIdTokenAuthTimeIsRecentIfPresent_UnitTest {
	private long nowSeconds;

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject claims;

	private CheckIdTokenAuthTimeIsRecentIfPresent cond;

	private JsonObject createToken(String json) {
		var claims = new JsonParser().parse(json).getAsJsonObject();

		JsonObject idToken = new JsonObject();
		idToken.add("claims", claims);

		return idToken;
	}

	@Before
	public void setUp() throws Exception {
		cond = new CheckIdTokenAuthTimeIsRecentIfPresent();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
		Date now = new Date();
		nowSeconds = now.getTime() / 1000;
	}

	@Test
	public void testEvaluate_good() {
		env.putObject("id_token", createToken("{ \"auth_time\": "+nowSeconds+" }"));

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_tooOld() {
		env.putObject("id_token", createToken("{ \"auth_time\": "+(nowSeconds-10*60)+" }"));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_noAuthTime() {
		env.putObject("id_token", createToken("{ }"));

		cond.execute(env);
	}

	@Test(expected = RuntimeException.class)
	public void testEvaluate_nullAuthTimes() {
		env.putObject("id_token", createToken("{ \"auth_time\": null }"));

		cond.execute(env);
	}

	@Test(expected = RuntimeException.class)
	public void testEvaluate_authTimeString() {
		env.putObject("id_token", createToken("{ \"auth_time\": \"16\" }"));

		cond.execute(env);
	}

}
