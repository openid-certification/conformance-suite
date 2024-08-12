package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
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
	@BeforeEach
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

	@Test
	public void testEvaluate_valueMissing() {
		assertThrows(ConditionError.class, () -> {

			JsonObject badToken = goodToken;
			badToken.get("claims").getAsJsonObject().remove("sub");
			env.putObject("id_token", badToken);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_nonprintable() {
		assertThrows(ConditionError.class, () -> {

			JsonObject badToken = goodToken;
			badToken.get("claims").getAsJsonObject().addProperty("sub", "fo\to");
			env.putObject("id_token", badToken);

			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_nonascii() {
		assertThrows(ConditionError.class, () -> {

			JsonObject badToken = goodToken;
			badToken.get("claims").getAsJsonObject().addProperty("sub", "\u007f");
			env.putObject("id_token", badToken);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_valueNull() {
		assertThrows(ConditionError.class, () -> {

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
		});
	}

}
