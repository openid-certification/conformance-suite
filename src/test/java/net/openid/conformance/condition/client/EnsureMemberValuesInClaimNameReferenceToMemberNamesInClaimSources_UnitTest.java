package net.openid.conformance.condition.client;

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
public class EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources_UnitTest {

	@Spy
	private Environment env = new Environment();

	@Mock
	private TestInstanceEventLog eventLog;

	private EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources cond;

	private JsonObject userInfo;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new EnsureMemberValuesInClaimNameReferenceToMemberNamesInClaimSources();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);

		userInfo = JsonParser.parseString("{"
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
		userInfo = JsonParser.parseString("""
				{
				   "name": "Jane Doe",
				   "given_name": "Jane",
				   "family_name": "Doe",
				   "birthdate": "0000-03-22",
				   "eye_color": "blue",
				   "email": "janedoe@example.com",
				   "_claim_names": {
				     "address": "src1",
				     "phone_number": "src1"
				   },
				   "_claim_sources": {
				     "src1": {"JWT": "jwt_header.jwt_part2.jwt_part3"}
				   }
				  }""").getAsJsonObject();
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test
	public void testEvaluateDistributedClaims_noError() {
		// example from https://openid.net/specs/openid-connect-core-1_0.html#DistributedExample
		userInfo = JsonParser.parseString("""
				{
				   "name": "Jane Doe",
				   "given_name": "Jane",
				   "family_name": "Doe",
				   "email": "janedoe@example.com",
				   "birthdate": "0000-03-22",
				   "eye_color": "blue",
				   "_claim_names": {
				     "payment_info": "src1",
				     "shipping_address": "src1",
				     "credit_score": "src2"
				    },
				   "_claim_sources": {
				     "src1": {"endpoint":
				                "https://bank.example.com/claim_source"},
				     "src2": {"endpoint":
				                "https://creditagency.example.com/claims_here",
				              "access_token": "ksj3n283dke"}
				   }
				  }""").getAsJsonObject();
		env.putObject("userinfo", userInfo);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_missingClaimNames() {
		assertThrows(ConditionError.class, () -> {
			// the spec isn't explicit but it seems that having only one of claim names and sources makes no sense
			userInfo.remove("_claim_names");
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_missingClaimSources() {
		assertThrows(ConditionError.class, () -> {
			// the spec isn't explicit but it seems that having only one of claim names and sources makes no sense
			userInfo.remove("_claim_sources");
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_invalid() {
		assertThrows(ConditionError.class, () -> {

			JsonObject claimSources = JsonParser.parseString("{"
				+ "\"payment_info\":\"src1\","
				+ "\"shipping_address\":\"src2\","
				+ "\"credit_score\":\"src3\""
				+ "}").getAsJsonObject();

			userInfo.add("_claim_sources", claimSources);
			env.putObject("userinfo", userInfo);
			cond.execute(env);
		});
	}

}
