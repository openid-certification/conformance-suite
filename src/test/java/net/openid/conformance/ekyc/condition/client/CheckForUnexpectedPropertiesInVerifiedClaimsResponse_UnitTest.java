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

	/**
	 * Runs the condition, asserts it warned, and returns the instance paths it reported as
	 * unknown properties - so a test can pin the exact set, not just that something was flagged.
	 */
	private List<String> unknownPropertyPaths(String claimsJson) {
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> mapCaptor = ArgumentCaptor.forClass(Map.class);
		assertThrows(ConditionError.class, () -> runTest(claimsJson));
		verify(eventLog, times(1)).log(anyString(), mapCaptor.capture());
		Object unknownProperties = mapCaptor.getValue().get("unknown_properties");
		return ((List<?>) unknownProperties).stream()
			.map(entry -> OIDFJSON.getString(((JsonObject) entry).get("path")))
			.toList();
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

	@Test
	public void testEvaluate_unknownPropertyInAttachment() {
		assertThrows(ConditionError.class, () -> runTest(EkycUnknownPropertyFixtures.RESPONSE_UNKNOWN_PROPERTY_IN_ATTACHMENT));
	}

	@Test
	public void testEvaluate_noWarningForAttachmentContentTypeWithParameter() {
		// Parameters are a valid part of a media type (RFC 6838, section 4.3), not unknown
		// properties.
		assertDoesNotThrow(() -> runTest("""
			{
			  "claims": {"given_name": "Paula"},
			  "verification": {
			    "trust_framework": "de_aml",
			    "evidence": [{
			      "type": "document",
			      "attachments": [{
			        "content_type": "text/plain; charset=utf-8",
			        "content": "aGVsbG8="
			      }]
			    }]
			  }
			}
			"""));
	}

	@Test
	public void testEvaluate_containerOfUnknownPropertyIsNotItselfReported_documentDetails() {
		// document_details is a spec-defined evidence member reached through an allOf/if/then
		// branch; the unknown property inside it must not drag document_details itself into
		// the warning as an "unevaluated" property.
		assertEquals(List.of("$.verified_claims.verification.evidence[0].document_details.personal_number"),
			unknownPropertyPaths(EkycUnknownPropertyFixtures.RESPONSE_UNKNOWN_PROPERTY_IN_DOCUMENT_DETAILS));
	}

	@Test
	public void testEvaluate_containerOfUnknownPropertyIsNotItselfReported_voucher() {
		assertEquals(List.of("$.verified_claims.verification.evidence[0].attestation.voucher.given_name"),
			unknownPropertyPaths(EkycUnknownPropertyFixtures.RESPONSE_UNKNOWN_PROPERTY_IN_VOUCHER));
	}

	@Test
	public void testEvaluate_noWarningWhenAttachmentMixesEmbeddedAndExternal() {
		// The structural validator (FAILURE) rejects this payload; all four members are
		// well-known attachment properties, so none of them may be misreported here as
		// unknown - even though each oneOf branch rejects the other branch's members.
		assertDoesNotThrow(() -> runTest(EkycUnknownPropertyFixtures.RESPONSE_ATTACHMENT_MIXING_EMBEDDED_AND_EXTERNAL));
	}

	@Test
	public void testEvaluate_noWarningWhenAttachmentMixesEmbeddedAndExternalWithInvalidContent() {
		// Same, with structurally invalid content: the embedded branch's genuine pattern error
		// must not make the external branch look like "the intended one" and turn
		// content_type/content into bogus unknown-property warnings.
		assertDoesNotThrow(() -> runTest(EkycUnknownPropertyFixtures.RESPONSE_ATTACHMENT_MIXING_EMBEDDED_AND_EXTERNAL_WITH_INVALID_CONTENT));
	}

	@Test
	public void testEvaluate_noWarningWhenAttachmentContentStructurallyInvalid() {
		// The structural validator (FAILURE) already rejects this payload; the other oneOf
		// branch's additionalProperties rejections of content_type/content must not surface
		// here as bogus "unknown properties".
		assertDoesNotThrow(() -> runTest(EkycUnknownPropertyFixtures.RESPONSE_ATTACHMENT_CONTENT_WITH_LINE_BREAK));
	}
}
