package io.fintechlabs.testframework.condition.common;

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
public class CheckCIBAServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodConfig;

	private JsonObject badConfigWithoutAuthorizationEndpoint;

	private JsonObject badConfigWithoutTokenEndpoint;

	private JsonObject badConfigWithoutIssuer;

	private CheckCIBAServerConfiguration cond;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckCIBAServerConfiguration();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodConfig = new JsonParser().parse("{"
			+ "\"backchannel_authentication_endpoint\":\"https://example.com/oauth/backauth\","
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
			+ "\"issuer\":\"ExampleApp\""
			+ "}").getAsJsonObject();

		badConfigWithoutAuthorizationEndpoint = new JsonParser().parse("{"
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
			+ "\"issuer\":\"ExampleApp\""
			+ "}").getAsJsonObject();

		badConfigWithoutTokenEndpoint = new JsonParser().parse("{"
			+ "\"backchannel_authentication_endpoint\":\"https://example.com/oauth/backauth\","
			+ "\"issuer\":\"ExampleApp\""
			+ "}").getAsJsonObject();

		badConfigWithoutIssuer = new JsonParser().parse("{"
			+ "\"backchannel_authentication_endpoint\":\"https://example.com/oauth/authorize\","
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\""
			+ "}").getAsJsonObject();
	}

	@Test
	public void testEvaluate_noError() {

		env.putObject("server", goodConfig);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("server", "backchannel_authentication_endpoint");
		verify(env, atLeastOnce()).getString("server", "token_endpoint");
		verify(env, atLeastOnce()).getString("server", "issuer");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingAuthorizationEndpoint() {

		env.putObject("server", badConfigWithoutAuthorizationEndpoint);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingTokenEndpoint() {

		env.putObject("server", badConfigWithoutTokenEndpoint);

		cond.evaluate(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIssuer() {

		env.putObject("server", badConfigWithoutIssuer);

		cond.evaluate(env);
	}

}
