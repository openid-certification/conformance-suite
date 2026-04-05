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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
			cond.execute(env);
			});
	}

	@Test
	public void testEvaluate_rejectUnknownPropertyInDocumentDetails() {
		assertThrows(ConditionError.class, () -> {
			JsonObject verifiedClaimsResponse = new JsonObject();
			String claimsJson = """
				{
				  "claims": {"given_name": "Paula"},
				  "verification": {
				    "trust_framework": "de_aml",
				    "evidence": [{
				      "type": "document",
				      "document_details": {
				        "type": "idcard",
				        "personal_number": "should-not-be-here"
				      }
				    }]
				  }
				}
				""";
			JsonObject parsedClaims = JsonParser.parseString(claimsJson).getAsJsonObject();
			verifiedClaimsResponse.add("id_token", parsedClaims);
			env.putObject("verified_claims_response", verifiedClaimsResponse);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_rejectUnknownPropertyInVoucher() {
		assertThrows(ConditionError.class, () -> {
			JsonObject verifiedClaimsResponse = new JsonObject();
			String claimsJson = """
				{
				  "claims": {"given_name": "Paula"},
				  "verification": {
				    "trust_framework": "de_aml",
				    "evidence": [{
				      "type": "vouch",
				      "attestation": {
				        "type": "written_attestation",
				        "voucher": {
				          "given_name": "should-use-name-not-given_name"
				        }
				      }
				    }]
				  }
				}
				""";
			JsonObject parsedClaims = JsonParser.parseString(claimsJson).getAsJsonObject();
			verifiedClaimsResponse.add("id_token", parsedClaims);
			env.putObject("verified_claims_response", verifiedClaimsResponse);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_rejectUnknownPropertyInAggregatedClaims() {
		assertThrows(ConditionError.class, () -> {
			JsonObject verifiedClaimsResponse = new JsonObject();
			String claimsJson = """
				{
				  "_claim_names": {"verified_claims": "src1"},
				  "_claim_sources": {
				    "src1": {
				      "JWT": "eyJ...",
				      "unknown_field": "should-not-be-here"
				    }
				  }
				}
				""";
			JsonObject parsedClaims = JsonParser.parseString(claimsJson).getAsJsonObject();
			verifiedClaimsResponse.add("id_token", parsedClaims);
			env.putObject("verified_claims_response", verifiedClaimsResponse);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_rejectUnknownPropertyInDistributedClaims() {
		assertThrows(ConditionError.class, () -> {
			JsonObject verifiedClaimsResponse = new JsonObject();
			String claimsJson = """
				{
				  "_claim_names": {"verified_claims": "src1"},
				  "_claim_sources": {
				    "src1": {
				      "endpoint": "https://example.com/claims",
				      "access_token": "token123",
				      "unknown_field": "should-not-be-here"
				    }
				  }
				}
				""";
			JsonObject parsedClaims = JsonParser.parseString(claimsJson).getAsJsonObject();
			verifiedClaimsResponse.add("id_token", parsedClaims);
			env.putObject("verified_claims_response", verifiedClaimsResponse);
			cond.execute(env);
		});
	}

	@Test
	public void testEvaluate_vouchRejectsWrongBranchFieldsAtEvidenceLevel() {
		// The evidence object uses allOf with if/then for conditional properties,
		// so unevaluatedProperties: false is used to reject fields from non-matching branches.
		assertThrows(ConditionError.class, () -> {
			JsonObject verifiedClaimsResponse = new JsonObject();
			String claimsJson = """
				{
				  "claims": {
				    "given_name": "Paula"
				  },
				  "verification": {
				    "trust_framework": "de_aml",
				    "evidence": [
				      {
				        "type": "vouch",
				        "document_details": "ignored-for-vouch"
				      }
				    ]
				  }
				}
				""";
			JsonObject parsedClaims = JsonParser.parseString(claimsJson).getAsJsonObject();
			verifiedClaimsResponse.add("id_token", parsedClaims);
			env.putObject("verified_claims_response", verifiedClaimsResponse);
			cond.execute(env);
		});
	}
}
