package net.openid.conformance.ekyc.condition.client;

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
class CheckForUnexpectedPropertiesInVerifiedClaimsRequest_UnitTest {
	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private CheckForUnexpectedPropertiesInVerifiedClaimsRequest cond;

	@BeforeEach
	void setUp() {
		cond = new CheckForUnexpectedPropertiesInVerifiedClaimsRequest();
		cond.setProperties("UNIT-TEST", eventLog, Condition.ConditionResult.INFO);
	}

	protected void runTest(String requestString) {
		env.putObject("authorization_endpoint_request", JsonParser.parseString(requestString).getAsJsonObject());
		cond.execute(env);
	}

	@Test
	public void testEvaluate_noError() {
		String request = """
			{
			  "claims": {
			    "id_token": {
			      "verified_claims": {
			        "claims": {"given_name": null},
			        "verification": {
			          "trust_framework": {"value": "de_aml"},
			          "evidence": [{
			            "type": {"value": "document"},
			            "document_details": {"type": null}
			          }]
			        }
			      }
			    }
			  }
			}
			""";

		assertDoesNotThrow(() -> runTest(request));
	}

	@Test
	public void testEvaluate_unknownPropertyInDocumentDetails() {
		assertThrows(ConditionError.class, () -> runTest(EkycUnknownPropertyFixtures.REQUEST_UNKNOWN_PROPERTY_IN_DOCUMENT_DETAILS));
	}

	@Test
	public void testEvaluate_unknownPropertyInCheckDetails() {
		assertThrows(ConditionError.class, () -> runTest(EkycUnknownPropertyFixtures.REQUEST_UNKNOWN_PROPERTY_IN_CHECK_DETAILS));
	}

	@Test
	public void testEvaluate_wrongBranchFieldAtEvidenceLevel() {
		assertThrows(ConditionError.class, () -> runTest(EkycUnknownPropertyFixtures.REQUEST_WRONG_BRANCH_FIELD_ON_VOUCH_EVIDENCE));
	}
}
