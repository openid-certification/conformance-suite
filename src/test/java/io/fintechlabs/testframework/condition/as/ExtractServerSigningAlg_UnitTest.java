package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ExtractServerSigningAlg_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject jwks;

	private JsonObject invalidJwks;

	private ExtractServerSigningAlg cond;

	private JsonObject combinedJwks;

	@Before
	public void setUp() throws Exception {

		cond = new ExtractServerSigningAlg();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO, new String[0]);

		jwks = new JsonParser().parse("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"PS256\","
			+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
			+ "}"
			+ "]}").getAsJsonObject();


		invalidJwks = new JsonParser().parse("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
			+ "}"
			+ "]}").getAsJsonObject();

		combinedJwks = new JsonParser().parse("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"PS256\","
			+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
			+ "}"
			+ ","
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"PS256\","
			+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
			+ "}"
			+ "]}" ).getAsJsonObject();
	}

	@Test
	public void testEvaluate_goodValues() {

		env.putObject("server_jwks", jwks);

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("server_jwks");

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_nullAlg(){

		env.putObject("server_jwks", invalidJwks);

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("server_jwks");

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_twoJWK(){

		env.putObject("server_jwks", combinedJwks);

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("server_jwks");
	}
}
