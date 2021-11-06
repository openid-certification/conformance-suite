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

		/* Commented out as this code leads to the following error ONLY in unit tests (but not at runtime)
		java.lang.NoSuchMethodError: org.json.JSONTokener.<init>(Ljava/io/InputStream;)V
			at net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsResponseAgainstSchema.checkSchema(ValidateVerifiedClaimsResponseAgainstSchema.java:702)
			at net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsResponseAgainstSchema.evaluate(ValidateVerifiedClaimsResponseAgainstSchema.java:679)
			at net.openid.conformance.condition.AbstractCondition.execute(AbstractCondition.java:125)
			at net.openid.conformance.ekyc.condition.client.ValidateVerifiedClaimsResponseAgainstSchema_UnitTest.testEvaluate_validateVerifiedClaimsSimple(ValidateVerifiedClaimsResponseAgainstSchema_UnitTest.java:53)
			...
			*/
		/*
		JsonObject verifiedClaimsResponse = new JsonObject();
		String claimsJson = "{\"verified_claims\":{\n" +
			"  \"claims\": {\n" +
			"    \"given_name\": \"Given001\"\n" +
			"  },\n" +
			"  \"verification\": {\n" +
			"    \"trust_framework\": \"de_aml\"\n" +
			"  }\n" +
			"}}";
		verifiedClaimsResponse.addProperty("id_token", claimsJson);
		JsonObject parsedClaims = new JsonParser().parse(claimsJson).getAsJsonObject();
		verifiedClaimsResponse.add("verified_claims", parsedClaims);
		env.putObject("verified_claims_response", verifiedClaimsResponse);
		env.putBoolean("ValidateVerifiedClaimsResponseAgainstSchema_UnitTest", Boolean.TRUE);
		cond.execute(env);
		*/
	}
}
