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
	public void testEvaluate_noError_essentialOnEvidenceType() {
		// IDA section 5.3: "RPs can use the essential field as defined in section 5.5.1 of the
		// OpenID Connect specification" - essential on evidence/type is not an unknown property,
		// and its presence must not stop the if/then discriminators from evaluating the
		// type-specific members (document_details here).
		String request = """
			{
			  "claims": {
			    "id_token": {
			      "verified_claims": {
			        "claims": {"given_name": null},
			        "verification": {
			          "trust_framework": {"value": "de_aml"},
			          "evidence": [{
			            "type": {"essential": true, "value": "document"},
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
	public void testEvaluate_essentialOnEvidenceTypeDoesNotMaskWrongBranchField() {
		// essential must not stop the document if/then from matching, so an
		// electronic_record-branch member on document evidence is still flagged.
		String request = """
			{
			  "claims": {
			    "id_token": {
			      "verified_claims": {
			        "claims": {"given_name": null},
			        "verification": {
			          "trust_framework": {"value": "de_aml"},
			          "evidence": [{
			            "type": {"essential": true, "value": "document"},
			            "record": {"type": null}
			          }]
			        }
			      }
			    }
			  }
			}
			""";

		assertThrows(ConditionError.class, () -> runTest(request));
	}

	@Test
	public void testEvaluate_unknownPropertyOnEvidenceType() {
		String request = """
			{
			  "claims": {
			    "id_token": {
			      "verified_claims": {
			        "claims": {"given_name": null},
			        "verification": {
			          "trust_framework": {"value": "de_aml"},
			          "evidence": [{
			            "type": {"value": "document", "valeu": "document"}
			          }]
			        }
			      }
			    }
			  }
			}
			""";

		assertThrows(ConditionError.class, () -> runTest(request));
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

	@Test
	public void testEvaluate_claimsNotAnObjectIsNotWarnedAboutHereOrACrash() {
		// A non-object claims member is a structural problem reported at FAILURE by
		// ValidateVerifiedClaimsRequestAgainstSchema; this condition runs with WARNING and must
		// neither duplicate that nor let a ClassCastException escape.
		assertDoesNotThrow(() -> runTest("""
			{"claims": "not-an-object"}
			"""));
	}
}
