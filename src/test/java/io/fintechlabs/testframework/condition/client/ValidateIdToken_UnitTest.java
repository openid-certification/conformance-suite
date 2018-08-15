package io.fintechlabs.testframework.condition.client;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ValidateIdToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private long nowSeconds;

	private JsonObject client;

	private String clientId;

	private JsonObject server;

	private JsonObject claims;

	private ValidateIdToken cond;

	/*
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ValidateIdToken("UNIT-TEST", eventLog, ConditionResult.INFO);

		Date now = new Date();
		nowSeconds = now.getTime() / 1000;
		long issuedAt = nowSeconds - 10; // pretend this came from a distant server

		clientId = "abc-client-id";

		client = new JsonParser().parse("{ \"client_id\": \"" + clientId + "\" }").getAsJsonObject();

		server = new JsonParser().parse("{"
			+ "\"issuer\":\"https://jwt-idp.example.com\""
			+ "}").getAsJsonObject();

		claims = new JsonParser().parse("{"
			+ "\"iss\":\"https://jwt-idp.example.com\","
			+ "\"sub\":\"mailto:mike@example.com\","
			+ "\"aud\":\"" + clientId + "\""
			+ "}").getAsJsonObject();
		claims.addProperty("exp", issuedAt + 300);
		claims.addProperty("nbf", issuedAt);
		claims.addProperty("iat", issuedAt);

	}

	private void addIdToken(Environment env, JsonObject claims) {

		JsonObject idToken = new JsonObject();
		idToken.add("claims", claims);
		env.put("id_token", idToken);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.put("client", client);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);

		verify(env, atLeastOnce()).getString("client", "client_id");
		verify(env, atLeastOnce()).getString("server", "issuer");
		verify(env, atLeastOnce()).getElementFromObject("id_token", "claims.iss");
		verify(env, atLeastOnce()).getElementFromObject("id_token", "claims.aud");
		verify(env, atLeastOnce()).getLong("id_token", "claims.exp");
		verify(env, atLeastOnce()).getLong("id_token", "claims.nbf");
		verify(env, atLeastOnce()).getLong("id_token", "claims.iat");

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingClientId() {

		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingServerConfig() {

		env.put("client", client);
		addIdToken(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIdToken() {

		env.put("client", client);
		env.put("server", server);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIssuer() {

		claims.remove("iss");

		env.put("client", client);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidIssuer() {

		claims.remove("iss");
		claims.addProperty("iss", "invalid");

		env.put("client", client);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingAudience() {

		claims.remove("aud");

		env.put("client", client);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidAudience() {

		claims.remove("aud");
		claims.addProperty("aud", "invalid");

		env.put("client", client);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test()
	public void testEvaluate_multipleAudience() {

		claims.remove("aud");
		JsonArray aud = new JsonArray();
		aud.add("https://other.example.com");
		aud.add(clientId);
		claims.add("aud", aud);

		env.put("client", client);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidMultipleAudience() {

		claims.remove("aud");
		JsonArray aud = new JsonArray();
		aud.add("https://other.example.com");
		aud.add("https://wheel.example.com");
		claims.add("aud", aud);

		env.put("client", client);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingExp() {

		claims.remove("exp");

		env.put("client", client);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidExp() {

		claims.remove("exp");
		claims.addProperty("exp", nowSeconds - (60 * 60)); // one hour in the past is not ok

		env.put("client", client);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_allowableExpSkew() {

		claims.remove("exp");
		claims.addProperty("exp", nowSeconds - (3 * 60)); // 3 minutes out should be fine still

		env.put("client", client);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIat() {

		claims.remove("iat");

		env.put("client", client);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidIat() {

		claims.remove("iat");
		claims.addProperty("iat", nowSeconds + 3600);

		env.put("client", client);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_allowableIatSkew() {

		claims.remove("iat");
		claims.addProperty("iat", nowSeconds + (3 * 60)); // 3 minutes out should be fine still

		env.put("client", client);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);

	}
}
