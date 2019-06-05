package io.fintechlabs.testframework.condition.as;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.Assert.assertEquals;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CopyAccessTokenFromASToClient_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject accessToken;

	private String accessTokenValue;

	private String tokenType;

	private CopyAccessTokenFromASToClient cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CopyAccessTokenFromASToClient();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		accessTokenValue = "foo1234556";
		tokenType = "Bearer";

		accessToken = new JsonParser().parse("{\n" +
				"\"type\": \"" + tokenType + "\",\n" +
				"\"value\": \"" + accessTokenValue + "\"\n" +
			"}").getAsJsonObject();

	}

	@Test
	public void testEvaluate() {

		env.putString("access_token", accessTokenValue);
		env.putString("token_type", tokenType);

		cond.evaluate(env);

		JsonObject res = env.getObject("access_token");

		assertEquals(accessToken, res);

	}


}
