package io.fintechlabs.testframework.condition.as;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.ConditionError;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;

@RunWith(MockitoJUnitRunner.class)
public class ValidateResourceAssertionSignature_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private JsonObject goodAssertion;

	private JsonObject badAssertion;

	private JsonObject goodResourceJwks;

	private JsonObject wrongResourceJwks;

	private ValidateResourceAssertionSignature cond;

	/*
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {

		cond = new ValidateResourceAssertionSignature();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		goodAssertion = new JsonParser().parse("{"
			+ "\"assertion\":"
			+ "eyJraWQiOiJyZXNvdXJjZS1rZXkiLCJhbGciOiJSUzI1NiIsInR"
			+ "5cCI6IkpXVCJ9.eyJzdWIiOiJwcm90ZWN0ZWQtcmVzb3VyY2UtMSIsImlzcyI6InByb3Rl"
			+ "Y3RlZC1yZXNvdXJjZS0xIiwiYXVkIjoiaHR0cHM6Ly9sb2NhbGhvc3Q6ODQ0My90ZXN0L2"
			+ "EvcnMtdGVzdC8iLCJleHAiOjE1MjY2ODI1MTYsImlhdCI6MTUyNjY4MjIxNiwianRpIjoi"
			+ "eWhiMWtyM002d1hSVzF3c2VSTWFFaVNJS0tsejNrczIifQ.ETixtUTR0A8mW7TcSl759fH"
			+ "hAogor9vQjduzx3sQ8mc8_Yr2xkw0Z0XJSKbRgHOPIttJNaKUTZD-iFG-1TWGqWuIiqeFR"
			+ "2JBymUMEwh9uSvCKWqqk0WtlAZYLJW8iDbmEqpUbTtmOLr_Iz_6PLd-WPKYsqb8NMMp9ay"
			+ "UY9iFRwaJBYK77Pf9QiRY2NotAlKw0oNYvIb10LscQue_DKbMLZdhU73eggSWBev_FbAm2"
			+ "Yt9Q2Uvk_tG1AsZA8IZdfZQCLZ4jCPHWWytcKM0w1Ygv625bQkwJKv-zZ5zaOi9855gdt5"
			+ "t1LuwDQfmswGdQ4U_8LjJUXaEIQ-ZFXvMlcI5HQ"
			+ "}").getAsJsonObject();

		badAssertion = new JsonParser().parse("{"
			+ "\"assertion\":\"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9."
			+ "eyJpc3MiOiJodHRwczovL2p3dC1pZHAuZXhhbXBsZS5jb20iLCJhdWQiOiJodHRwczovL290aGVyLmV4YW1wbGUubmV0IixleHA6MCxuYmY6MCxpYXQ6MH0."
			+ "fqJ5UNpPd47z9CO2u9DQBr+7bxS3PeAUzAV/C/3eGDY\""
			+ "}").getAsJsonObject();

		goodResourceJwks = new JsonParser().parse("{"
			+ "\"keys\":["
			+ "{\n" +
			"  \"kty\": \"RSA\",\n" +
			"  \"e\": \"AQAB\",\n" +
			"  \"use\": \"sig\",\n" +
			"  \"kid\": \"resource-key\",\n" +
			"  \"alg\": \"RS256\",\n" +
			"  \"n\": \"lhKkoXeMB2YWXk79EXAuJSOp5rMP3f8O_R1SjziMDg9b-SgcVYzfthvgub9Ri8s97-rZWO-i_IV5ZbOOHkX55Br1Yf_eW4r-YpC455OmD-QrAmSNILfGip7Lqf-NtgCzwsLF2dSnWCA5w7s6B8H1dRn6zFr33Cx_5Lvj6kdnncf0hdX9mGG80a7QuJxEYEB3vVtZupCgfB4tPV9NAzk3mEQc8qXJRVrHKe-nkKctwsWPeWZ-jht5X8LAYQtDOUAET4-9Hx7h8YeTnjE5t5_re1RLnTqa4kegMKuSh1PulVcRErD68VL8BthvoeADXnSq7BCR2SZlSdOpN6-NtXxGhQ\"\n" +
			"}"
			+ "]}").getAsJsonObject();

		wrongResourceJwks = new JsonParser().parse("{"
			+ "\"keys\":["
			+ "{\n" +
			"  \"alg\": \"RS256\",\n" +
			"  \"e\": \"AQAB\",\n" +
			"  \"n\": \"p8eP5gL1H_H9UNzCuQS-vNRVz3NWxZTHYk1tG9VpkfFjWNKG3MFTNZJ1l5g_COMm2_2i_YhQNH8MJ_nQ4exKMXrWJB4tyVZohovUxfw-eLgu1XQ8oYcVYW8ym6Um-BkqwwWL6CXZ70X81YyIMrnsGTyTV6M8gBPun8g2L8KbDbXR1lDfOOWiZ2ss1CRLrmNM-GRp3Gj-ECG7_3Nx9n_s5to2ZtwJ1GS1maGjrSZ9GRAYLrHhndrL_8ie_9DS2T-ML7QNQtNkg2RvLv4f0dpjRYI23djxVtAylYK4oiT_uEMgSkc4dxwKwGuBxSO0g9JOobgfy0--FUHHYtRi0dOFZw\",\n" +
			"  \"kty\": \"RSA\",\n" +
			"  \"kid\": \"authserver\"\n" +
			"}"
			+ "]}").getAsJsonObject();

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test
	public void testEvaluate_noError() {

		env.putObject("resource_assertion", goodAssertion);
		env.putObject("resource_public_jwks", goodResourceJwks);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badToken() {

		env.putObject("resource_assertion", badAssertion);
		env.putObject("resource_public_jwks", goodResourceJwks);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingToken() {

		env.putObject("resource_public_jwks", goodResourceJwks);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_wrongKeys() {

		env.putObject("resource_assertion", goodAssertion);
		env.putObject("resource_public_jwks", wrongResourceJwks);

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_badKeys() {

		env.putObject("resource_assertion", goodAssertion);
		env.putString("resource_public_jwks", "this is not a key set");

		cond.evaluate(env);

	}

	/**
	 * Test method for {@link io.fintechlabs.testframework.condition.client.ValidateIdTokenSignature#evaluate(io.fintechlabs.testframework.testmodule.Environment)}.
	 */
	@Test(expected = ConditionError.class)
	public void testEvaluate_missingKeys() {

		env.putObject("resource_assertion", goodAssertion);

		cond.evaluate(env);

	}
}
