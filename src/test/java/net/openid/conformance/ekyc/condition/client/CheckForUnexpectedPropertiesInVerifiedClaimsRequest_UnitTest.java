package net.openid.conformance.ekyc.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.ConditionError;
import net.openid.conformance.logging.BsonEncoding;
import net.openid.conformance.logging.TestInstanceEventLog;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

	/**
	 * Runs the condition, asserts it warned, and returns the instance paths it reported as
	 * unknown properties - so a test can pin the exact set, not just that something was flagged.
	 */
	private List<String> unknownPropertyPaths(String requestString) {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
		assertThrows(ConditionError.class, () -> runTest(requestString));
		verify(eventLog, times(1)).log(anyString(), mapCaptor.capture());
		Object unknownProperties = mapCaptor.getValue().get("unknown_properties");
		return ((List<?>) unknownProperties).stream()
			.map(entry -> OIDFJSON.getString(((JsonObject) entry).get("path")))
			.toList();
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
		// document_details is a spec-defined evidence member reached through an allOf/if/then
		// branch; the unknown property inside it must not drag document_details itself into
		// the warning as an "unevaluated" property.
		assertEquals(List.of("$.id_token.verified_claims.verification.evidence[0].document_details.personal_number"),
			unknownPropertyPaths(EkycUnknownPropertyFixtures.REQUEST_UNKNOWN_PROPERTY_IN_DOCUMENT_DETAILS));
	}

	@Test
	public void testEvaluate_unknownPropertyInCheckDetails() {
		assertEquals(List.of("$.id_token.verified_claims.verification.evidence[0].check_details[0].unknown_field"),
			unknownPropertyPaths(EkycUnknownPropertyFixtures.REQUEST_UNKNOWN_PROPERTY_IN_CHECK_DETAILS));
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
