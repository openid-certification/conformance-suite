package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ValidateVerifiedClaimsResponseAgainstSchema_UnitTest
{
	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private ValidateVerifiedClaimsResponseAgainstSchema cond;

	@Before
	public void setUp() throws Exception {
		cond = new ValidateVerifiedClaimsResponseAgainstSchema();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

	}

	@Test
	public void testEvaluate_validateVerifiedClaimsSimple() {
		JsonObject verifiedClaimsResponse = new JsonObject();
		String claimsJson = "{\"claims\":{\"given_name\":\"Paula\"},\"verification\":{\"trust_framework\":\"de_aml\"}}";
		JsonObject parsedClaims = new JsonParser().parse(claimsJson).getAsJsonObject();
		verifiedClaimsResponse.add("id_token", parsedClaims);
		env.putObject("verified_claims_response", verifiedClaimsResponse);
		env.putBoolean("ValidateVerifiedClaimsResponseAgainstSchema_UnitTest", Boolean.TRUE);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_validateVerifiedClaimsError() {
		JsonObject verifiedClaimsResponse = new JsonObject();
		String claimsJson = "{\"foo_claims\":{\"given_name\":\"Paula\"},\"verification\":{\"trust_framework\":\"de_aml\"}}";
		JsonObject parsedClaims = new JsonParser().parse(claimsJson).getAsJsonObject();
		verifiedClaimsResponse.add("id_token", parsedClaims);
		env.putObject("verified_claims_response", verifiedClaimsResponse);
		env.putBoolean("ValidateVerifiedClaimsResponseAgainstSchema_UnitTest", Boolean.TRUE);
		cond.execute(env);
	}
}
