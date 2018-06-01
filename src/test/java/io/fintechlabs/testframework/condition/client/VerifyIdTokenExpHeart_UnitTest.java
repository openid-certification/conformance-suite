package io.fintechlabs.testframework.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

/**
 * @author srmoore
 */
@RunWith(MockitoJUnitRunner.class)
public class VerifyIdTokenExpHeart_UnitTest {
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private VerifyIdTokenExpHeart cond;

	@Before
	public void setUp() throws Exception {
		cond = new VerifyIdTokenExpHeart("UNIT_TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void createIdToken(Environment env, int seconds) {
		Date now = new Date();
		long nowSeconds = now.getTime() / 1000;
		long issuedAt = nowSeconds - 10; // pretend this came from a distant server

		JsonObject claims = new JsonParser().parse("{"
			+ "\"iss\":\"https://jwt-idp.example.com\","
			+ "\"sub\":\"mailto:mike@example.com\","
			+ "\"aud\":\"https://jwt-rp.example.net\""
			+ "}").getAsJsonObject();
		claims.addProperty("iat", issuedAt);
		claims.addProperty("exp", issuedAt + seconds);

		JsonObject idToken = new JsonObject();
		idToken.add("claims", claims);
		env.put("id_token",idToken);
	}

	/**
	 * Test method for {@link VerifyIdTokenExpHeart#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_noError() {
		createIdToken(env,240); // 4 minute offset
		cond.evaluate(env);
		verify(env, atLeastOnce()).getLong("id_token", "claims.exp");
		verify(env, atLeastOnce()).getLong("id_token", "claims.iat");
	}

	/**
	 * Test method for {@link VerifyIdTokenExpHeart#evaluate(Environment)}
	 */
	@Test
	public void testEvaluate_noErrorMaxLength() {
		createIdToken(env,300); // 5 minute offset
		cond.evaluate(env);
	}

	/**
	 * Test method for {@link VerifyIdTokenExpHeart#evaluate(Environment)}
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_tooLong() {
		createIdToken(env,600); // 10 minute offset
		cond.evaluate(env);
	}

	/**
	 * Test method for {@link VerifyIdTokenExpHeart#evaluate(Environment)}
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_noExp() {
		Date now = new Date();
		long nowSeconds = now.getTime() / 1000;
		long issuedAt = nowSeconds - 10; // pretend this came from a distant server

		JsonObject claims = new JsonParser().parse("{"
			+ "\"iss\":\"https://jwt-idp.example.com\","
			+ "\"sub\":\"mailto:mike@example.com\","
			+ "\"aud\":\"https://jwt-rp.example.net\""
			+ "}").getAsJsonObject();
		claims.addProperty("iat", issuedAt);

		JsonObject idToken = new JsonObject();
		idToken.add("claims", claims);
		env.put("id_token",idToken);
		cond.evaluate(env);
	}

	/**
	 * Test method for {@link VerifyIdTokenExpHeart#evaluate(Environment)}
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_noIat() {
		Date now = new Date();
		long nowSeconds = now.getTime() / 1000;
		long issuedAt = nowSeconds - 10; // pretend this came from a distant server

		JsonObject claims = new JsonParser().parse("{"
			+ "\"iss\":\"https://jwt-idp.example.com\","
			+ "\"sub\":\"mailto:mike@example.com\","
			+ "\"aud\":\"https://jwt-rp.example.net\""
			+ "}").getAsJsonObject();
		claims.addProperty("exp", issuedAt);

		JsonObject idToken = new JsonObject();
		idToken.add("claims", claims);
		env.put("id_token",idToken);
		cond.evaluate(env);
	}

	/**
	 * Test method for {@link VerifyIdTokenExpHeart#evaluate(Environment)}
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_iatAfterExp() {
		Date now = new Date();
		long nowSeconds = now.getTime() / 1000;
		long issuedAt = nowSeconds - 10; // pretend this came from a distant server

		JsonObject claims = new JsonParser().parse("{"
			+ "\"iss\":\"https://jwt-idp.example.com\","
			+ "\"sub\":\"mailto:mike@example.com\","
			+ "\"aud\":\"https://jwt-rp.example.net\""
			+ "}").getAsJsonObject();
		claims.addProperty("exp", issuedAt);
		claims.addProperty("iat", issuedAt + 300);

		JsonObject idToken = new JsonObject();
		idToken.add("claims", claims);
		env.put("id_token",idToken);
		cond.evaluate(env);
	}

}
