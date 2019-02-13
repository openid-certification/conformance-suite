package io.fintechlabs.testframework.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.logging.TestInstanceEventLog;
import io.fintechlabs.testframework.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class AddCHashToIdTokenClaims_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddCHashToIdTokenClaims cond;

	private String hash;

	private JsonObject claims;

	@Before
	public void setUp() throws Exception {

		cond = new AddCHashToIdTokenClaims("UNIT-TEST", eventLog, ConditionResult.INFO);

		claims = new JsonParser().parse("{"
			+ "\"iss\":\"https://localhost:8443/test/a/fintech-clienttest\","
			+ "\"sub\":\"user-subject-12345431\","
			+ "\"aud\":\"test-clinet-id-123\","
			+ "\"nonce\":\"123abcdef\","
			+ "\"openbanking_intent_id\":\"ABC123DEF456\""
			+ "}").getAsJsonObject();

		hash = new JsonParser().parse("40PTS2Jr3ezaQXA_T0BN_A").getAsString();
	}

	@Test
	public void testEvaluate_addHash() {

		env.putObject("id_token_claims", claims);

		env.putString("c_hash", hash);

		claims = new JsonObject();
		claims.addProperty("c_hash", hash);

		cond.evaluate(env);

		assertNotNull(env.getObject("id_token_claims" ));
		assertEquals(hash, env.getElementFromObject("id_token_claims", "c_hash").getAsString());
	}


	@Test(expected = NullPointerException.class)
	public void testEvaluate_valueMissing() {

		cond.evaluate(env);

	}

}
