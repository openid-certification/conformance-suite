package io.fintechlabs.testframework.condition.as;

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

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ValidateResourceAssertionClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private long nowSeconds;

	private String resourceId;

	private String serverIssuer;

	private JsonObject server;

	private JsonObject claims;

	private ValidateResourceAssertionClaims cond;

	/*
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ValidateResourceAssertionClaims("UNIT-TEST", eventLog, ConditionResult.INFO);

		Date now = new Date();
		nowSeconds = now.getTime() / 1000;
		long issuedAt = nowSeconds - 10; // pretend this came from a distant server

		resourceId = "resource-id-1";

		serverIssuer = "https://idp.example.com/";

		server = new JsonParser().parse("{"
			+ "\"issuer\":\"" + serverIssuer + "\","
			+ "\"introspection_endpoint\":\"" + serverIssuer + "introspect\""
			+ "}").getAsJsonObject();

		claims = new JsonParser().parse("{"
			+ "\"iss\":\"" + resourceId + "\","
			+ "\"sub\":\"" + resourceId + "\","
			+ "\"aud\":\"" + serverIssuer + "\""
			+ "}").getAsJsonObject();
		claims.addProperty("exp", issuedAt + 300);
		claims.addProperty("nbf", issuedAt);
		claims.addProperty("iat", issuedAt);

		env.putString("resource_id", resourceId);
		env.putString("issuer", serverIssuer);
		env.putObject("server", server);
	}

	private void addAssertion(Environment env, JsonObject claims) {

		JsonObject assertion = new JsonObject();
		assertion.add("assertion_payload", claims);
		env.putObject("resource_assertion", assertion);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		addAssertion(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIssuer() {

		claims.remove("iss");

		addAssertion(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidIssuer() {

		claims.remove("iss");
		claims.addProperty("iss", "invalid");

		addAssertion(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingAudience() {

		claims.remove("aud");

		addAssertion(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidAudience() {

		claims.remove("aud");
		claims.addProperty("aud", "invalid");

		addAssertion(env, claims);

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
		aud.add(serverIssuer);
		claims.add("aud", aud);

		addAssertion(env, claims);

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

		addAssertion(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingExp() {

		claims.remove("exp");

		addAssertion(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidExp() {

		claims.remove("exp");
		claims.addProperty("exp", nowSeconds - (60 * 60)); // one hour in the past is not ok

		addAssertion(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_allowableExpSkew() {

		claims.remove("exp");
		claims.addProperty("exp", nowSeconds - (3 * 60)); // 3 minutes out should be fine still

		addAssertion(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIat() {

		claims.remove("iat");

		addAssertion(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidIat() {

		claims.remove("iat");
		claims.addProperty("iat", nowSeconds + 3600);

		addAssertion(env, claims);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_allowableIatSkew() {

		claims.remove("iat");
		claims.addProperty("iat", nowSeconds + (3 * 60)); // 3 minutes out should be fine still

		addAssertion(env, claims);

		cond.evaluate(env);

	}
}
