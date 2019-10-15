package net.openid.conformance.condition.as;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

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

		cond = new AddCHashToIdTokenClaims();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		claims = new JsonParser().parse("{"
			+ "\"iss\":\"https://localhost:8443/test/a/fintech-clienttest\","
			+ "\"sub\":\"user-subject-12345431\","
			+ "\"aud\":\"test-clinet-id-123\","
			+ "\"nonce\":\"123abcdef\","
			+ "\"openbanking_intent_id\":\"ABC123DEF456\""
			+ "}").getAsJsonObject();

		hash = OIDFJSON.getString((new JsonParser().parse("40PTS2Jr3ezaQXA_T0BN_A")));
	}

	@Test
	public void testEvaluate_addHash() {

		env.putObject("id_token_claims", claims);

		env.putString("c_hash", hash);

		claims = new JsonObject();
		claims.addProperty("c_hash", hash);

		cond.execute(env);

		assertNotNull(env.getObject("id_token_claims" ));
		assertEquals(hash, env.getString("id_token_claims", "c_hash"));
	}


	@Test(expected = ConditionError.class)
	public void testEvaluate_valueMissing() {

		cond.execute(env);

	}

}
