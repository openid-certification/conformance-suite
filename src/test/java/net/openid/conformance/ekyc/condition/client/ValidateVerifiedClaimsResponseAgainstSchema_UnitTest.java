package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
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
		verifiedClaimsResponse.addProperty("location", "id_token");
		String claimsJson = "{\"verified_claims\":{\n" +
			"  \"claims\": {\n" +
			"    \"given_name\": \"Given001\"\n" +
			"  },\n" +
			"  \"verification\": {\n" +
			"    \"trust_framework\": \"de_aml\"\n" +
			"  }\n" +
			"}}";
		JsonObject parsedClaims = new JsonParser().parse(claimsJson).getAsJsonObject();
		verifiedClaimsResponse.add("verified_claims", parsedClaims);
		env.putObject("verified_claims_response", verifiedClaimsResponse);
		env.putBoolean("ValidateVerifiedClaimsResponseAgainstSchema_UnitTest", Boolean.TRUE);
		cond.execute(env);

	}
}
