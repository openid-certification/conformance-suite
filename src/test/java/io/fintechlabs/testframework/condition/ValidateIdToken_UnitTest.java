package io.fintechlabs.testframework.condition;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

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

import io.fintechlabs.testframework.logging.EventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ValidateIdToken_UnitTest {
	
	@Spy
	private Environment env = new Environment();
	
	@Mock
	private EventLog eventLog;
	
	private long nowSeconds;
	
	private String clientId;
	
	private JsonObject server;
	
	private JsonObject claims;
	
	private ValidateIdToken cond;
	
	/*
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		
		cond = new ValidateIdToken("UNIT-TEST", eventLog, false);
		
		Date now = new Date();
		nowSeconds = now.getTime() / 1000;
		long issuedAt = nowSeconds - 10; // pretend this came from a distant server
		
		clientId = "https://jwt-rp.example.net";
		
		server = new JsonParser().parse("{"
				+ "\"issuer\":\"https://jwt-idp.example.com\""
				+ "}").getAsJsonObject();
		
		claims = new JsonParser().parse("{"
				+ "\"iss\":\"https://jwt-idp.example.com\","
				+ "\"sub\":\"mailto:mike@example.com\","
				+ "\"aud\":\"https://jwt-rp.example.net\""
				+ "}").getAsJsonObject();
		claims.addProperty("exp", issuedAt + 300);
		claims.addProperty("nbf", issuedAt);
		claims.addProperty("iat", issuedAt);
		
	}
	
	private void addIdToken(Environment env, JsonObject claims) {
		addIdToken(env, claims, null);
	}
	
	private void addIdToken(Environment env, JsonObject claims, JsonObject header) {
		
		JsonObject idToken = new JsonObject();
		idToken.add("claims", claims);
		if (header != null) {
			idToken.add("header",  header);
		}
		env.put("id_token", idToken);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {
		
		env.putString("client_id", clientId);
		env.put("server", server);
		addIdToken(env, claims);
		
		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("client_id");
		verify(env, atLeastOnce()).getString("server", "issuer");
		verify(env, atLeastOnce()).findElement("id_token", "claims.iss");
		verify(env, atLeastOnce()).findElement("id_token", "claims.aud");
		verify(env, atLeastOnce()).getLong("id_token", "claims.exp");
		verify(env, atLeastOnce()).getLong("id_token", "claims.nbf");
		verify(env, atLeastOnce()).getLong("id_token", "claims.iat");
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_withHash() {
		
		claims.addProperty("s_hash", "WZRHGrsBESr8wYFZ9sx0tA");
		
		JsonObject header = new JsonParser().parse("{\"alg\":\"HS256\"}").getAsJsonObject();
		
		env.putString("client_id", clientId);
		env.put("server", server);
		env.putString("state", "12345");
		addIdToken(env, claims, header);
		
		cond.evaluate(env);
		
		verify(env, atLeastOnce()).getString("id_token", "claims.s_hash");
		verify(env, atLeastOnce()).getString("state");
		verify(env, atLeastOnce()).getString("id_token", "header.alg");
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingClientId() {
		
		env.put("server", server);
		addIdToken(env, claims);
		
		cond.evaluate(env);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingServerConfig() {
		
		env.putString("client_id", clientId);
		addIdToken(env, claims);
		
		cond.evaluate(env);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIdToken() {
		
		env.putString("client_id", clientId);
		env.put("server", server);
		
		cond.evaluate(env);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIssuer() {
		
		claims.remove("iss");
		
		env.putString("client_id", clientId);
		env.put("server", server);
		addIdToken(env, claims);
		
		cond.evaluate(env);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidIssuer() {
		
		claims.remove("iss");
		claims.addProperty("iss", "invalid");
		
		env.putString("client_id", clientId);
		env.put("server", server);
		addIdToken(env, claims);
		
		cond.evaluate(env);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingAudience() {
		
		claims.remove("aud");
		
		env.putString("client_id", clientId);
		env.put("server", server);
		addIdToken(env, claims);
		
		cond.evaluate(env);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidAudience() {
		
		claims.remove("aud");
		claims.addProperty("aud", "invalid");
		
		env.putString("client_id", clientId);
		env.put("server", server);
		addIdToken(env, claims);
		
		cond.evaluate(env);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test()
	public void testEvaluate_multipleAudience() {
		
		claims.remove("aud");
		JsonArray aud = new JsonArray();
		aud.add("https://other.example.com");
		aud.add(clientId);
		claims.add("aud", aud);
		
		env.putString("client_id", clientId);
		env.put("server", server);
		addIdToken(env, claims);
		
		cond.evaluate(env);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidMultipleAudience() {
		
		claims.remove("aud");
		JsonArray aud = new JsonArray();
		aud.add("https://other.example.com");
		aud.add("https://wheel.example.com");
		claims.add("aud", aud);
		
		env.putString("client_id", clientId);
		env.put("server", server);
		addIdToken(env, claims);
		
		cond.evaluate(env);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingExp() {
		
		claims.remove("exp");
		
		env.putString("client_id", clientId);
		env.put("server", server);
		addIdToken(env, claims);
		
		cond.evaluate(env);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidExp() {
		
		claims.remove("exp");
		claims.addProperty("exp", nowSeconds - (60 * 60)); // one hour in the past is not ok
		
		env.putString("client_id", clientId);
		env.put("server", server);
		addIdToken(env, claims);
		
		cond.evaluate(env);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_allowableExpSkew() {
		
		claims.remove("exp");
		claims.addProperty("exp", nowSeconds - (3 * 60)); // 3 minutes out should be fine still

		env.putString("client_id", clientId);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingIat() {
		
		claims.remove("iat");
		
		env.putString("client_id", clientId);
		env.put("server", server);
		addIdToken(env, claims);
		
		cond.evaluate(env);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_invalidIat() {
		
		claims.remove("iat");
		claims.addProperty("iat", nowSeconds + 3600);
		
		env.putString("client_id", clientId);
		env.put("server", server);
		addIdToken(env, claims);
		
		cond.evaluate(env);
		
	}
	
	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_allowableIatSkew() {
		
		claims.remove("iat");
		claims.addProperty("iat", nowSeconds + (3 * 60)); // 3 minutes out should be fine still

		env.putString("client_id", clientId);
		env.put("server", server);
		addIdToken(env, claims);

		cond.evaluate(env);
		
	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.ValidateIdToken#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badHash() {
		
		claims.addProperty("s_hash", "WZRHGrsBESr8wYFZ9sx0tA");
		
		JsonObject header = new JsonParser().parse("{\"alg\":\"HS256\"}").getAsJsonObject();
		
		env.putString("client_id", clientId);
		env.put("server", server);
		env.putString("state", "abcde");
		addIdToken(env, claims, header);
		
		cond.evaluate(env);
	}
}
