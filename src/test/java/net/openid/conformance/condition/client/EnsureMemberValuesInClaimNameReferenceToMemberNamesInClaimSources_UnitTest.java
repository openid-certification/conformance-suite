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
	public void testEvaluate_missingClaimNames() {
		userInfo.remove("_claim_names");
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingClaimSources() {
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
