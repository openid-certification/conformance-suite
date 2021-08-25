package net.openid.conformance.condition.common;

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

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class CheckServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodConfig;

	private JsonObject badConfigWithoutAuthorizationEndpoint;

	private JsonObject badConfigWithoutTokenEndpoint;

	private JsonObject badConfigWithoutIssuer;

	private CheckServerConfiguration cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckServerConfiguration();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodConfig = new JsonParser().parse("{"
			+ "\"authorization_endpoint\":\"https://example.com/oauth/authorize\","
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
			+ "\"issuer\":\"https://example.com/\""
			+ "}").getAsJsonObject();

		badConfigWithoutAuthorizationEndpoint = new JsonParser().parse("{"
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
			+ "\"issuer\":\"https://example.com/\""
			+ "}").getAsJsonObject();

		badConfigWithoutTokenEndpoint = new JsonParser().parse("{"
			+ "\"authorization_endpoint\":\"https://example.com/oauth/authorize\","
			+ "\"issuer\":\"https://example.com/\""
			+ "}").getAsJsonObject();

		badConfigWithoutIssuer = new JsonParser().parse("{"
			+ "\"authorization_endpoint\":\"https://example.com/oauth/authorize\","
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\""
			+ "}").getAsJsonObject();
	}

	/**
	 * Test method for {@link CheckServerConfiguration#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putObject("server", goodConfig);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("server", "authorization_endpoint");
		verify(env, atLeastOnce()).getString("server", "token_endpoint");
		verify(env, atLeastOnce()).getString("server", "issuer");
	}

	/**
	 * Test method for {@link CheckServerConfiguration#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingAuthorizationEndpoint() {

		env.putObject("server", badConfigWithoutAuthorizationEndpoint);

		cond.execute(env);
	}

	/**
	 * Test method for {@link CheckServerConfiguration#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingTokenEndpoint() {

		env.putObject("server", badConfigWithoutTokenEndpoint);

		cond.execute(env);
	}

	/**
	 * Test method for {@link CheckServerConfiguration#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIssuer() {

		env.putObject("server", badConfigWithoutIssuer);

		cond.execute(env);
	}
}
