package io.fintechlabs.testframework.condition.client;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CheckForSubjectInIdToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodToken;

	private CheckForSubjectInIdToken cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckForSubjectInIdToken();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Good sample from OpenID Connect Core spec

		JsonObject goodClaims = new JsonParser().parse("{\n" +
			" \"iss\": \"http://server.example.com\",\n" +
			" \"sub\": \"248289761001\",\n" +
			" \"aud\": \"s6BhdRkqt3\",\n" +
			" \"nonce\": \"n-0S6_WzA2Mj\",\n" +
			" \"exp\": 1311281970,\n" +
			" \"iat\": 1311280970\n" +
			"}").getAsJsonObject();

		goodToken = new JsonObject();
		goodToken.add("claims", goodClaims);
	}

	/**
	 * Test method for {@link CheckForSubjectInIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("id_token", goodToken);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("id_token", "claims.sub");
	}

	/**
	 * Test method for {@link CheckForSubjectInIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		JsonObject badToken = goodToken;
		badToken.get("claims").getAsJsonObject().remove("sub");
		env.putObject("id_token", badToken);

		cond.evaluate(env);
	}

}
