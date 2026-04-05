package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
public class CheckForUnexpectedPropertiesInVerifiedClaimsResponse_UnitTest
{
	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckForUnexpectedPropertiesInVerifiedClaimsResponse cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new CheckForUnexpectedPropertiesInVerifiedClaimsResponse();

		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	private void runTest(String claimsJson) {
		JsonObject verifiedClaimsResponse = new JsonObject();
		JsonObject parsedClaims = JsonParser.parseString(claimsJson).getAsJsonObject();
		verifiedClaimsResponse.add("id_token", parsedClaims);
		env.putObject("verified_claims_response", verifiedClaimsResponse);
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noError() {
		assertDoesNotThrow(() -> runTest("""
			{
			  "claims": {"given_name": "Paula"},
			  "verification": {
			    "trust_framework": "de_aml",
			    "evidence": [{
			      "type": "document",
			      "document_details": {"type": "idcard"}
			    }]
			  }
			}
			"""));
	}

	@Test
	public void testEvaluate_unknownPropertyInDocumentDetails() {
		assertThrows(ConditionError.class, () -> runTest(EkycUnknownPropertyFixtures.RESPONSE_UNKNOWN_PROPERTY_IN_DOCUMENT_DETAILS));
	}

	@Test
	public void testEvaluate_unknownPropertyInVoucher() {
		assertThrows(ConditionError.class, () -> runTest(EkycUnknownPropertyFixtures.RESPONSE_UNKNOWN_PROPERTY_IN_VOUCHER));
	}

	@Test
	public void testEvaluate_wrongBranchFieldAtEvidenceLevel() {
		assertThrows(ConditionError.class, () -> runTest(EkycUnknownPropertyFixtures.RESPONSE_WRONG_BRANCH_FIELD_ON_VOUCH_EVIDENCE));
	}
}
