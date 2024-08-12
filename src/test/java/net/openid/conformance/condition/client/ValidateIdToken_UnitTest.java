package net.openid.conformance.condition.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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
	@BeforeEach
	public void setUp() throws Exception {

		cond = new ValidateIdToken();
		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		Date now = new Date();
		nowSeconds = now.getTime() / 1000;
		long issuedAt = nowSeconds - 10; // pretend this came from a distant server

		clientId = "abc-client-id";

		client = JsonParser.parseString("{ \"client_id\": \"" + clientId + "\" }").getAsJsonObject();

		server = JsonParser.parseString("{"
			+ "\"issuer\":\"https://jwt-idp.example.com\""
			+ "}").getAsJsonObject();

		claims = JsonParser.parseString("{"
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
	@Test
	public void testEvaluate_missingClientId() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("server", server);
			env.putObject("client", new JsonObject());
			addIdToken(env, claims);

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingServerConfig() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("client", client);
			addIdToken(env, claims);

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingIdToken() {
		assertThrows(ConditionError.class, () -> {

			env.putObject("client", client);
			env.putObject("server", server);

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingIssuer() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("iss");

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidIssuer() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("iss");
			claims.addProperty("iss", "invalid");

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingAudience() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("aud");

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidAudience() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("aud");
			claims.addProperty("aud", "invalid");

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test
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
	@Test
	public void testEvaluate_invalidMultipleAudience() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("aud");
			JsonArray aud = new JsonArray();
			aud.add("https://other.example.com");
			aud.add("https://wheel.example.com");
			claims.add("aud", aud);

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_missingExp() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("exp");

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidExp() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("exp");
			claims.addProperty("exp", nowSeconds - (60 * 60)); // one hour in the past is not ok

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);

		});

	}

	// this doesn't error as per comment in implementation
	@Test
	public void testEvaluate_invalidNbf() {

		claims.remove("nbf");
		claims.addProperty("nbf", nowSeconds + (60 * 60));

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
	@Test
	public void testEvaluate_missingIat() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("iat");

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);

		});

	}

	/**
	 * Test method for {@link ValidateIdToken#evaluate(Environment)}.
	 */
	@Test
	public void testEvaluate_invalidIat() {
		assertThrows(ConditionError.class, () -> {

			claims.remove("iat");
			claims.addProperty("iat", nowSeconds + 3600);

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);

		});

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

	public void testEvaluate_authTime() {
		claims.addProperty("auth_time", nowSeconds - 3600);

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_invalidAuthTimeFuture() {
		assertThrows(ConditionError.class, () -> {
			claims.addProperty("auth_time", nowSeconds + 3600);

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_invalidAuthTimePast() {
		assertThrows(ConditionError.class, () -> {
			claims.addProperty("auth_time", nowSeconds - 2 * 365 * 24 * 60 * 60);

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_invalidAuthTimeString() {
		assertThrows(ConditionError.class, () -> {
			claims.addProperty("auth_time", String.valueOf(nowSeconds - 3600));

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_invalidExpString() {
		assertThrows(ConditionError.class, () -> {

			JsonElement o = claims.remove("exp");
			claims.addProperty("exp", String.valueOf(OIDFJSON.getNumber(o))); // a string (containing a valid number) is not ok

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);

		});

	}

	public void testEvaluate_acr() {
		claims.addProperty("acr", "0");

		env.putObject("client", client);
		env.putObject("server", server);
		addIdToken(env, claims);

		cond.execute(env);
	}

	@Test
	public void testEvaluate_acrEmpty() {
		assertThrows(ConditionError.class, () -> {
			claims.addProperty("acr", "");

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_acrArray() {
		assertThrows(ConditionError.class, () -> {
			claims.add("acr", JsonParser.parseString("[ \"foo\" ]"));

			env.putObject("client", client);
			env.putObject("server", server);
			addIdToken(env, claims);

			cond.execute(env);
		});
	}

}
