package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class CheckForSubjectInIdToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodToken;

	private CheckForSubjectInIdToken cond;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new CheckForSubjectInIdToken();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		// Good sample from OpenID Connect Core spec

		JsonObject goodClaims = JsonParser.parseString("""
				{
				 "iss": "http://server.example.com",
				 "sub": "248289761001",
				 "aud": "s6BhdRkqt3",
				 "nonce": "n-0S6_WzA2Mj",
				 "exp": 1311281970,
				 "iat": 1311280970
				}""").getAsJsonObject();

		goodToken = new JsonObject();
		goodToken.add("claims", goodClaims);
	}

	@Test
	public void testEvaluate_valuePresent() {

		env.putObject("id_token", goodToken);

		cond.execute(env);

		verify(env, atLeastOnce()).getString("id_token", "claims.sub");
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		JsonObject badToken = goodToken;
		badToken.get("claims").getAsJsonObject().remove("sub");
		env.putObject("id_token", badToken);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_nonprintable() {

		JsonObject badToken = goodToken;
		badToken.get("claims").getAsJsonObject().addProperty("sub", "fo\to");
		env.putObject("id_token", badToken);

		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_nonascii() {

		JsonObject badToken = goodToken;
		badToken.get("claims").getAsJsonObject().addProperty("sub", "\u007f");
		env.putObject("id_token", badToken);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_valueNull() {

		JsonObject nullClaims = JsonParser.parseString("""
				{
				 "iss": "http://server.example.com",
				 "sub": null,
				 "aud": "s6BhdRkqt3",
				 "nonce": "n-0S6_WzA2Mj",
				 "exp": 1311281970,
				 "iat": 1311280970
				}""").getAsJsonObject();

		var tokenWithNull = new JsonObject();
		tokenWithNull.add("claims", nullClaims);

		env.putObject("id_token", tokenWithNull);

		cond.execute(env);
	}

}
