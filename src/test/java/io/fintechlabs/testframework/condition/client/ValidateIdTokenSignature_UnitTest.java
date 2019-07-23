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
public class ValidateIdTokenSignature_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodIdToken;

	private JsonObject goodIdTokenWithKid;

	private JsonObject badIdToken;

	private JsonObject goodServerJwks;

	private JsonObject wrongServerJwks;

	private JsonObject wrongServerJwksWithKid;

	private ValidateIdTokenSignature cond;

	/*
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ValidateIdTokenSignature();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodIdToken = new JsonParser().parse("{"
			+ "\"value\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
			+ "eyJpc3MiOiJodHRwczovL2p3dC1pZHAuZXhhbXBsZS5jb20iLCJhdWQiOiJodHRwczovL2p3dC1ycC5leGFtcGxlLm5ldCIsZXhwOjAsbmJmOjAsaWF0OjB9."
			+ "fqJ5UNpPd47z9CO2u9DQBr+7bxS3PeAUzAV/C/3eGDY\""
			+ "}").getAsJsonObject();

		badIdToken = new JsonParser().parse("{"
			+ "\"value\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
			+ "eyJpc3MiOiJodHRwczovL2p3dC1pZHAuZXhhbXBsZS5jb20iLCJhdWQiOiJodHRwczovL290aGVyLmV4YW1wbGUubmV0IixleHA6MCxuYmY6MCxpYXQ6MH0."
			+ "fqJ5UNpPd47z9CO2u9DQBr+7bxS3PeAUzAV/C/3eGDY\""
			+ "}").getAsJsonObject();

		// { "kid": "authlete-fapidev-api-20180524", "alg": "PS256" }
		goodIdTokenWithKid = new JsonParser().parse("{"
			+ "\"value\":\"eyJraWQiOiJhdXRobGV0ZS1mYXBpZGV2LWFwaS0yMDE4MDUyNCIsImFsZyI6IlBTMjU2In0."
			+ "eyJpc3MiOiJodHRwczovL2p3dC1pZHAuZXhhbXBsZS5jb20iLCJhdWQiOiJodHRwczovL290aGVyLmV4YW1wbGUubmV0IixleHA6MCxuYmY6MCxpYXQ6MH0."
			+ "fqJ5UNpPd47z9CO2u9DQBr+7bxS3PeAUzAV/C/3eGDY\""
			+ "}").getAsJsonObject();

		goodServerJwks = new JsonParser().parse("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"HS256\","
			+ "\"k\":\"UzUgc1C/vF44Uf9jZuswyJrivNwGas6uVYhVEi7GKUQ\""
			+ "},"
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"HS256\","
			+ "\"k\":\"LAEuRo0oklLIyw/md746i3ZhbCPd4UoZ7+J421/avfM\""
			+ "}"
			+ "]}").getAsJsonObject();

		wrongServerJwks = new JsonParser().parse("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"HS256\","
			+ "\"k\":\"UzUgc1C/vF44Uf9jZuswyJrivNwGas6uVYhVEi7GKUQ\""
			+ "}"
			+ "]}").getAsJsonObject();

		wrongServerJwksWithKid = new JsonParser().parse("{"
			+ "\"keys\":["
			+ "{"
			+ "\"kty\":\"oct\","
			+ "\"alg\":\"HS256\","
			+ "\"k\":\"UzUgc1C/vF44Uf9jZuswyJrivNwGas6uVYhVEi7GKUQ\","
			+ "\"kid\":\"authlete-fapidev-api-20180524\""
			+ "}"
			+ "]}").getAsJsonObject();

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putObject("id_token", goodIdToken);
		env.putObject("server_jwks", goodServerJwks);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("id_token", "value");
		verify(env, atLeastOnce()).getObject("server_jwks");

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badToken() {

		env.putObject("id_token", badIdToken);
		env.putObject("server_jwks", goodServerJwks);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("id_token", "value");
		verify(env, atLeastOnce()).getObject("server_jwks");

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingToken() {

		env.putObject("server_jwks", goodServerJwks);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("id_token", "value");
		verify(env, atLeastOnce()).getObject("server_jwks");

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_wrongKeys() {

		env.putObject("id_token", goodIdToken);
		env.putObject("server_jwks", wrongServerJwks);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("id_token", "value");
		verify(env, atLeastOnce()).getObject("server_jwks");

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badKeys() {

		env.putObject("id_token", goodIdToken);
		env.putString("server_jwks", "this is not a key set");

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("id_token", "value");
		verify(env, atLeastOnce()).getObject("server_jwks");

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingKeys() {

		env.putObject("id_token", goodIdToken);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("id_token", "value");
		verify(env, atLeastOnce()).getObject("server_jwks");

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_notFoundKeyWithKid() {

		env.putObject("id_token", goodIdTokenWithKid);

		env.putObject("server_jwks", goodServerJwks);

		cond.evaluate(env);

	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_foundKeyWithKidAndVerifyFailure() {

		env.putObject("id_token", goodIdTokenWithKid);

		env.putObject("server_jwks", wrongServerJwksWithKid);

		cond.evaluate(env);

	}

}
