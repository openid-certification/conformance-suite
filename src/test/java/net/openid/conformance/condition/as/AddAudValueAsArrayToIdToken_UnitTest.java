package net.openid.conformance.condition.as;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AddAudValueAsArrayToIdToken_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private AddAudValueAsArrayToIdToken cond;

	private String aud;

	private JsonObject claims;

	@BeforeEach
	public void setUp() throws Exception {

		cond = new AddAudValueAsArrayToIdToken();

		cond.setProperties("UNIT-TEST", eventLog, ConditionResult.INFO);

		aud = "test-client-id-123";

		claims = JsonParser.parseString("{"
			+ "\"iss\":\"https://localhost:8443/test/a/fintech-clienttest\","
			+ "\"sub\":\"user-subject-12345431\","
			+ "\"nonce\":\"123abcdef\","
			+ "\"openbanking_intent_id\":\"ABC123DEF456\""
			+ "}").getAsJsonObject();

		claims.addProperty("aud", aud);

		env.putObject("id_token_claims", claims);

	}

	@Test
	public void testEvaluate_noError() {

		JsonArray audArray = new JsonArray();
		audArray.add("test-client-id-123");

		cond.execute(env);

		verify(env, atLeastOnce()).getObject("id_token_claims");
		assertEquals(audArray, env.getElementFromObject("id_token_claims", "aud").getAsJsonArray());

	}

}
