package io.fintechlabs.testframework.condition.common;

import io.fintechlabs.testframework.condition.ConditionError;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

/**
 * @author srmoore
 */
@RunWith(MockitoJUnitRunner.class)
public class CheckHeartServerConfiguration_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodConfig;

	private JsonObject badConfigWithoutAuthorizationEndpoint;

	private JsonObject badConfigWithoutTokenEndpoint;

	private JsonObject badConfigWithoutIssuer;

	private JsonObject badConfigWithoutRevocationEndpoint;

	private JsonObject badConfigWithoutIntrospectionEndpoint;

	private JsonObject badConfigWithoutJwksUri;

	private JsonObject badConfigWithBadIssuer;

	private CheckHeartServerConfiguration cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckHeartServerConfiguration("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodConfig = new JsonParser().parse("{"
			+ "\"authorization_endpoint\":\"https://example.com/oauth/authorize\","
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
			+ "\"issuer\":\"https://example.com\","
			+ "\"introspection_endpoint\":\"https://example.com/introspection\","
			+ "\"revocation_endpoint\":\"https://example.com/revoke\","
			+ "\"jwks_uri\":\"https://example.com/jwks\""
			+ "}").getAsJsonObject();

		badConfigWithoutAuthorizationEndpoint = new JsonParser().parse("{"
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
			+ "\"issuer\":\"https://example.com\","
			+ "\"introspection_endpoint\":\"https://example.com/introspection\","
			+ "\"revocation_endpoint\":\"https://example.com/revoke\","
			+ "\"jwks_uri\":\"https://example.com/jwks\""
			+ "}").getAsJsonObject();

		badConfigWithoutTokenEndpoint = new JsonParser().parse("{"
			+ "\"authorization_endpoint\":\"https://example.com/oauth/authorize\","
			+ "\"issuer\":\"https://example.com\","
			+ "\"introspection_endpoint\":\"https://example.com/introspection\","
			+ "\"revocation_endpoint\":\"https://example.com/revoke\","
			+ "\"jwks_uri\":\"https://example.com/jwks\""
			+ "}").getAsJsonObject();

		badConfigWithoutIssuer = new JsonParser().parse("{"
			+ "\"authorization_endpoint\":\"https://example.com/oauth/authorize\","
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
			+ "\"introspection_endpoint\":\"https://example.com/introspection\","
			+ "\"revocation_endpoint\":\"https://example.com/revoke\","
			+ "\"jwks_uri\":\"https://example.com/jwks\""
			+ "}").getAsJsonObject();

		badConfigWithoutIntrospectionEndpoint = new JsonParser().parse("{"
			+ "\"authorization_endpoint\":\"https://example.com/oauth/authorize\","
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
			+ "\"issuer\":\"https://example.com\","
			+ "\"revocation_endpoint\":\"https://example.com/revoke\","
			+ "\"jwks_uri\":\"https://example.com/jwks\""
			+ "}").getAsJsonObject();

		badConfigWithoutRevocationEndpoint = new JsonParser().parse("{"
			+ "\"authorization_endpoint\":\"https://example.com/oauth/authorize\","
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
			+ "\"issuer\":\"https://example.com\","
			+ "\"introspection_endpoint\":\"https://example.com/introspection\","
			+ "\"jwks_uri\":\"https://example.com/jwks\""
			+ "}").getAsJsonObject();

		badConfigWithoutJwksUri = new JsonParser().parse("{"
			+ "\"authorization_endpoint\":\"https://example.com/oauth/authorize\","
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
			+ "\"issuer\":\"https://example.com\","
			+ "\"introspection_endpoint\":\"https://example.com/introspection\","
			+ "\"revocation_endpoint\":\"https://example.com/revoke\""
			+ "}").getAsJsonObject();

		badConfigWithBadIssuer = new JsonParser().parse("{"
			+ "\"authorization_endpoint\":\"https://example.com/oauth/authorize\","
			+ "\"token_endpoint\":\"https://example.com/api/oauth/token\","
			+ "\"issuer\":\"ExampleApp\","
			+ "\"introspection_endpoint\":\"https://example.com/introspection\","
			+ "\"revocation_endpoint\":\"https://example.com/revoke\","
			+ "\"jwks_uri\":\"https://example.com/jwks\""
			+ "}").getAsJsonObject();


	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.CheckServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.put("server", goodConfig);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("server", "authorization_endpoint");
		verify(env, atLeastOnce()).getString("server", "token_endpoint");
		verify(env, atLeastOnce()).getString("server", "issuer");
		verify(env, atLeastOnce()).getString("server", "introspection_endpoint");
		verify(env, atLeastOnce()).getString("server", "revocation_endpoint");
		verify(env, atLeastOnce()).getString("server", "jwks_uri");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.CheckServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingAuthorizationEndpoint() {

		env.put("server", badConfigWithoutAuthorizationEndpoint);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.CheckServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badAuthorizationEndpoint() {
		goodConfig.remove("authorization_endpoint");
		goodConfig.addProperty("authorization_endpoint", "ExampleApp");
		env.put("server", goodConfig);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.CheckServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingTokenEndpoint() {

		env.put("server", badConfigWithoutTokenEndpoint);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.CheckServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badTokenEndpoint() {
		goodConfig.remove("token_endpoint");
		goodConfig.addProperty("token_endpoint", "ExampleApp");

		env.put("server", goodConfig);

		cond.evaluate(env);
	}


	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.CheckServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIssuer() {

		env.put("server", badConfigWithoutIssuer);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.CheckServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badIssuer() {

		env.put("server", badConfigWithBadIssuer);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.CheckServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIntrospectionEndpoint() {

		env.put("server", badConfigWithoutIntrospectionEndpoint);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.CheckServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badIntrospectionEndpoint() {
		goodConfig.remove("introspection_endpoint");
		goodConfig.addProperty("introspection_endpoint", "ExampleApp");

		env.put("server", goodConfig);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.CheckServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingRevocationEndpoint() {

		env.put("server", badConfigWithoutRevocationEndpoint);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.CheckServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badRevocationEndpoint() {
		goodConfig.remove("revocation_endpoint");
		goodConfig.addProperty("revocation_endpoint", "ExampleApp");

		env.put("server", goodConfig);

		cond.evaluate(env);
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.CheckServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingJwksUri() {

		env.put("server", badConfigWithoutJwksUri);

		cond.evaluate(env);
	}


	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.common.CheckServerConfiguration#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badJwksUri() {
		goodConfig.remove("jwks_uri");
		goodConfig.addProperty("jwks_uri", "ExampleApp");

		env.put("server", goodConfig);

		cond.evaluate(env);
	}

}
