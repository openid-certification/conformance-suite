package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
public class ValidateTokenResponseNotIncludeCHashAndSHash_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateTokenResponseNotIncludeCHashAndSHash cond;

	@Before
	public void setUp() throws Exception {

		cond = new ValidateTokenResponseNotIncludeCHashAndSHash("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

	}

	@Test
	public void testEvalutate_isGood() {
		// Define claims object
		JsonObject claims = new JsonParser().parse("{\n" +
			"  \"at_hash\": \"SzqRJ7WtQMjkoDyPMXnpvA\",\n" +
			"  \"sub\": \"1001\",\n" +
			"  \"aud\": \"21541757519\",\n" +
			"  \"auth_time\": 1553590905,\n" +
			"  \"iss\": \"https://fapidev-as.authlete.net/\",\n" +
			"  \"exp\": 1553591207,\n" +
			"  \"iat\": 1553590907\n" +
			"}").getAsJsonObject();

		JsonObject o = new JsonObject();
		o.add("claims", claims);

		env.putObject("id_token", o);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvalutate_isBad() {
		// Define claims object
		JsonObject claims = new JsonParser().parse("{\n" +
			"  \"at_hash\": \"SzqRJ7WtQMjkoDyPMXnpvA\",\n" +
			"  \"sub\": \"1001\",\n" +
			"  \"aud\": \"21541757519\",\n" +
			"  \"auth_time\": 1553590905,\n" +
			"  \"iss\": \"https://fapidev-as.authlete.net/\",\n" +
			"  \"exp\": 1553591207,\n" +
			"  \"iat\": 1553590907,\n" +
			"  \"s_hash\": \"1553590907\"\n" +
			"}").getAsJsonObject();

		JsonObject o = new JsonObject();
		o.add("claims", claims);

		env.putObject("id_token", o);

		cond.evaluate(env);
	}

}
