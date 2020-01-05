package net.openid.conformance.condition.client;

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

@RunWith(MockitoJUnitRunner.class)
public class EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources cond;

	private JsonObject userInfo;

	@Before
	public void setUp() throws Exception {
		cond = new EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		userInfo = new JsonParser().parse("{"
			+ "\"_claim_names\": {"
				+ "\"payment_info\":\"src1\","
				+ "\"shipping_address\":\"src1\","
				+ "\"credit_score\":\"src2\""
			+ "},"
			+ "\"_claim_sources\": {"
				+ "\"src1\": {"
					+ "\"endpoint\":\"https://bank.example.com/claim_source\""
				+ "},"
				+ "\"src2\": {"
					+ "\"endpoint\":\"https://creditagency.example.com/claims_here\","
					+ "\"access_token\":\"ksj3n283dke\""
				+ "}"
				+ "}"
			+ "}").getAsJsonObject();
	}

	@Test
	public void testEvaluate_noError() {
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test
	public void testEvaluateAggregatedClaims_noError() {
		// example from https://openid.net/specs/openid-connect-core-1_0.html#AggregatedExample
		userInfo = new JsonParser().parse("{\n" +
			"   \"name\": \"Jane Doe\",\n" +
			"   \"given_name\": \"Jane\",\n" +
			"   \"family_name\": \"Doe\",\n" +
			"   \"birthdate\": \"0000-03-22\",\n" +
			"   \"eye_color\": \"blue\",\n" +
			"   \"email\": \"janedoe@example.com\",\n" +
			"   \"_claim_names\": {\n" +
			"     \"address\": \"src1\",\n" +
			"     \"phone_number\": \"src1\"\n" +
			"   },\n" +
			"   \"_claim_sources\": {\n" +
			"     \"src1\": {\"JWT\": \"jwt_header.jwt_part2.jwt_part3\"}\n" +
			"   }\n" +
			"  }").getAsJsonObject();
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test
	public void testEvaluateDistributedClaims_noError() {
		// example from https://openid.net/specs/openid-connect-core-1_0.html#DistributedExample
		userInfo = new JsonParser().parse("{\n" +
			"   \"name\": \"Jane Doe\",\n" +
			"   \"given_name\": \"Jane\",\n" +
			"   \"family_name\": \"Doe\",\n" +
			"   \"email\": \"janedoe@example.com\",\n" +
			"   \"birthdate\": \"0000-03-22\",\n" +
			"   \"eye_color\": \"blue\",\n" +
			"   \"_claim_names\": {\n" +
			"     \"payment_info\": \"src1\",\n" +
			"     \"shipping_address\": \"src1\",\n" +
			"     \"credit_score\": \"src2\"\n" +
			"    },\n" +
			"   \"_claim_sources\": {\n" +
			"     \"src1\": {\"endpoint\":\n" +
			"                \"https://bank.example.com/claim_source\"},\n" +
			"     \"src2\": {\"endpoint\":\n" +
			"                \"https://creditagency.example.com/claims_here\",\n" +
			"              \"access_token\": \"ksj3n283dke\"}\n" +
			"   }\n" +
			"  }").getAsJsonObject();
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingClaimNames() {
		// the spec isn't explicit but it seems that having only one of claim names and sources makes no sense
		userInfo.remove("_claim_names");
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_missingClaimSources() {
		// the spec isn't explicit but it seems that having only one of claim names and sources makes no sense
		userInfo.remove("_claim_sources");
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test(expected = ConditionError.class)
	public void testEvaluate_invalid() {

		JsonObject claimSources = new JsonParser().parse("{"
			+ "\"payment_info\":\"src1\","
			+ "\"shipping_address\":\"src2\","
			+ "\"credit_score\":\"src3\""
			+ "}").getAsJsonObject();

		userInfo.add("_claim_sources", claimSources);
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

}
