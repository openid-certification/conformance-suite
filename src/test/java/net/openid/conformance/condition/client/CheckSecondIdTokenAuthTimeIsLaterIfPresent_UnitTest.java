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

@RunWith(MockitoJUnitRunner.class)
public class CheckSecondIdTokenAuthTimeIsLaterIfPresent_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject claims;

	private CheckSecondIdTokenAuthTimeIsLaterIfPresent cond;

	private JsonObject createToken(String json) {
		var claims = new JsonParser().parse(json).getAsJsonObject();

		JsonObject idToken = new JsonObject();
		idToken.add("claims", claims);

		return idToken;
	}

	@Before
	public void setUp() throws Exception {
		cond = new CheckSecondIdTokenAuthTimeIsLaterIfPresent();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);
	}

	@Test
	public void testEvaluate_secondHasLaterAuthTime() {
		env.putObject("first_id_token", createToken("{ \"auth_time\": 15 }"));
		env.putObject("id_token", createToken("{ \"auth_time\": 16 }"));

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_secondHasEarlierAuthTime() {
		env.putObject("first_id_token", createToken("{ \"auth_time\": 15 }"));
		env.putObject("id_token", createToken("{ \"auth_time\": 14 }"));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_noAuthTimes() {
		env.putObject("first_id_token", createToken("{ }"));
		env.putObject("id_token", createToken("{ }"));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_onlyFirstAuthTime() {
		env.putObject("first_id_token", createToken("{ \"auth_time\": 15 }"));
		env.putObject("id_token", createToken("{ }"));

		cond.execute(env);
	}

	@Test
	public void testEvaluate_onlySecondAuthTime() {
		env.putObject("first_id_token", createToken("{ }"));
		env.putObject("id_token", createToken("{ \"auth_time\": 16 }"));

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_sameAuthTimes() {
		env.putObject("first_id_token", createToken("{ \"auth_time\": 15 }"));
		env.putObject("id_token", createToken("{ \"auth_time\": 15 }"));

		cond.execute(env);
	}

	@Test(expected = RuntimeException.class)
	public void testEvaluate_nullAuthTimes() {
		env.putObject("first_id_token", createToken("{ \"auth_time\": null }"));
		env.putObject("id_token", createToken("{ \"auth_time\": 15 }"));

		cond.execute(env);
	}

	@Test(expected = RuntimeException.class)
	public void testEvaluate_authTimeStrings() {
		env.putObject("first_id_token", createToken("{ \"auth_time\": \"15\" }"));
		env.putObject("id_token", createToken("{ \"auth_time\": \"16\" }"));

		cond.execute(env);
	}

}
