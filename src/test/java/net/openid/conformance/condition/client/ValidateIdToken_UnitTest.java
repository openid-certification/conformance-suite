package net.openid.conformance.condition.client;

import java.util.Date;

import com.google.gson.JsonElement;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;

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

		cond = new ValidateIdToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

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
		env.putObject("id_token", idToken);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("client", "client_id");
		verify(env, atLeastOnce()).getString("server", "issuer");
		verify(env, atLeastOnce()).getElementFromObject("id_token", "claims.iss");
		verify(env, atLeastOnce()).getElementFromObject("id_token", "claims.aud");
		verify(env, atLeastOnce()).getLong("id_token", "claims.exp");
		verify(env, atLeastOnce()).getLong("id_token", "claims.nbf");
		verify(env, atLeastOnce()).getLong("id_token", "claims.iat");

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingClientId() {

		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingServerConfig() {

		env.putObject("client", client);
		addIdToken(env, claims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIdToken() {

		env.putObject("client", client);
		env.putObject("server", server);

		cond.execute(env);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIssuer() {

		claims.remove("iss");

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidIssuer() {

		claims.remove("iss");
		claims.addProperty("iss", "invalid");

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingAudience() {

		claims.remove("aud");

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidAudience() {

		claims.remove("aud");
		claims.addProperty("aud", "invalid");

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test()
	public void testEvaluate_multipleAudience() {

		claims.remove("aud");
		JsonArray aud = new JsonArray();
		aud.add("https://other.example.com");
		aud.add(clientId);
		claims.add("aud", aud);

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidMultipleAudience() {

		claims.remove("aud");
		JsonArray aud = new JsonArray();
		aud.add("https://other.example.com");
		aud.add("https://wheel.example.com");
		claims.add("aud", aud);

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingExp() {

		claims.remove("exp");

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidExp() {

		claims.remove("exp");
		claims.addProperty("exp", nowSeconds - (60 * 60)); // one hour in the past is not ok

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_allowableExpSkew() {

		claims.remove("exp");
		claims.addProperty("exp", nowSeconds - (3 * 60)); // 3 minutes out should be fine still

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIat() {

		claims.remove("iat");

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidIat() {

		claims.remove("iat");
		claims.addProperty("iat", nowSeconds + 3600);

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_allowableIatSkew() {

		claims.remove("iat");
		claims.addProperty("iat", nowSeconds + (3 * 60)); // 3 minutes out should be fine still

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

	}

	@Test(expected = IllegalArgumentException.class)
	public void testEvaluate_invalidExpString() {

		JsonElement o = claims.remove("exp");
		claims.addProperty("exp", String.valueOf(OIDFJSON.getNumber(o))); // a string (containing a valid number) is not ok

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);

	}

}
