package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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

@ExtendWith(MockitoExtension.class)
public class ValidateVerifiedClaimsResponseAgainstSchema_UnitTest
{
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateVerifiedClaimsResponseAgainstSchema cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateVerifiedClaimsResponseAgainstSchema();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_validateVerifiedClaimsSimple() {
		JsonObject verifiedClaimsResponse = new JsonObject();
		String claimsJson = "{\"claims\":{\"given_name\":\"Paula\"},\"verification\":{\"trust_framework\":\"de_aml\"}}";
		JsonObject parsedClaims = JsonParser.parseString(claimsJson).getAsJsonObject();
		verifiedClaimsResponse.add("id_token", parsedClaims);
		env.putObject("verified_claims_response", verifiedClaimsResponse);
		env.putBoolean("ValidateVerifiedClaimsResponseAgainstSchema_UnitTest", Boolean.TRUE);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_validateVerifiedClaimsError() {
		assertThrows(ConditionError.class, () -> {
			JsonObject verifiedClaimsResponse = new JsonObject();
			String claimsJson = "{\"foo_claims\":{\"given_name\":\"Paula\"},\"verification\":{\"trust_framework\":\"de_aml\"}}";
			JsonObject parsedClaims = JsonParser.parseString(claimsJson).getAsJsonObject();
			verifiedClaimsResponse.add("id_token", parsedClaims);
			env.putObject("verified_claims_response", verifiedClaimsResponse);
			env.putBoolean("ValidateVerifiedClaimsResponseAgainstSchema_UnitTest", Boolean.TRUE);
			cond.execute(env);
		});
	}
}
