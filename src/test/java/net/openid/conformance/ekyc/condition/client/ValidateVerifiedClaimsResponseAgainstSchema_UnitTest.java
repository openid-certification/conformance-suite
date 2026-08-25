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
public class ValidateVerifiedClaimsResponseAgainstSchema_UnitTest
{
	@Spy
	private Environment env = new Environment();

	private final TestInstanceEventLog eventLog = BsonEncoding.testInstanceEventLog();

	private ValidateVerifiedClaimsResponseAgainstSchema cond;

	@BeforeEach
	public void setUp() throws Exception {
		cond = new ValidateVerifiedClaimsResponseAgainstSchema();

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
	public void testEvaluate_validateVerifiedClaimsSimple() {
		runTest("{\"claims\":{\"given_name\":\"Paula\"},\"verification\":{\"trust_framework\":\"de_aml\"}}");
	}

	@Test
	public void testEvaluate_validateVerifiedClaimsError() {
		assertThrows(ConditionError.class, () ->
			runTest("{\"foo_claims\":{\"given_name\":\"Paula\"},\"verification\":{\"trust_framework\":\"de_aml\"}}"));
	}

	@Test
	public void testEvaluate_unknownPropertyInDocumentDetailsIsNotAStructuralFailure() {
		// Unknown properties are reported (as a warning) by
		// CheckForUnexpectedPropertiesInVerifiedClaimsResponse, not by this condition.
		assertDoesNotThrow(() -> runTest(EkycUnknownPropertyFixtures.RESPONSE_UNKNOWN_PROPERTY_IN_DOCUMENT_DETAILS));
	}

	@Test
	public void testEvaluate_unknownPropertyInVoucherIsNotAStructuralFailure() {
		assertDoesNotThrow(() -> runTest(EkycUnknownPropertyFixtures.RESPONSE_UNKNOWN_PROPERTY_IN_VOUCHER));
	}

	@Test
	public void testEvaluate_validateVerifiedClaimsVouchCanContainDocumentDetailsWithoutDocumentBranchValidation() {
		// Fields from a non-matching evidence branch are unevaluated properties; they are
		// reported (as a warning) by CheckForUnexpectedPropertiesInVerifiedClaimsResponse,
		// not by this condition.
		assertDoesNotThrow(() -> runTest(EkycUnknownPropertyFixtures.RESPONSE_WRONG_BRANCH_FIELD_ON_VOUCH_EVIDENCE));
	}

	@Test
	public void testEvaluate_unknownPropertyInAttachmentIsNotAStructuralFailure() {
		assertDoesNotThrow(() -> runTest(EkycUnknownPropertyFixtures.RESPONSE_UNKNOWN_PROPERTY_IN_ATTACHMENT));
	}

	@Test
	public void testEvaluate_allowedAttachmentContentTypeWithParameter() {
		// RFC 6838 section 4.3 defines parameters as part of a media type's specification, and
		// the attachments spec does not forbid them.
		assertDoesNotThrow(() -> executeWithAttachmentContentType("text/plain; charset=utf-8"));
	}

	@Test
	public void testEvaluate_allowedAttachmentContentTypeWithQuotedParameter() {
		// the value is substituted into a JSON string literal, so the quotes are JSON-escaped
		assertDoesNotThrow(() -> executeWithAttachmentContentType("text/plain; charset=\\\"utf-8\\\""));
	}

	@Test
	public void testEvaluate_allowedAttachmentContentTypeWithEscapedQuoteInParameterValue() {
		// RFC 7230 quoted-pair: quoted-string parameter values may contain backslash-escaped
		// characters, including the double quote itself
		assertDoesNotThrow(() -> executeWithAttachmentContentType("text/plain; name=\\\"a\\\\\\\"b\\\""));
	}

	@Test
	public void testEvaluate_allowedAttachmentContentTypeWithExtendedParameterName() {
		// RFC 2231/RFC 8187 extended parameters use a name ending in '*'; '*' and "'" are
		// legal token characters (RFC 7231), even though RFC 6838's restricted-name set
		// (which only governs registered type/subtype names) excludes them
		assertDoesNotThrow(() -> executeWithAttachmentContentType("text/plain; charset*=UTF-8''x"));
	}

	@Test
	public void testEvaluate_allowedAttachmentContentTypeWithMultipleParameters() {
		assertDoesNotThrow(() -> executeWithAttachmentContentType("application/vnd.example; profile=basic; version=2"));
	}

	@Test
	public void testEvaluate_allowedAttachmentContentTypeUppercase() {
		// Media type names are case-insensitive (RFC 6838, section 4.2)
		assertDoesNotThrow(() -> executeWithAttachmentContentType("IMAGE/PNG"));
	}

	@Test
	public void testEvaluate_rejectForbiddenAttachmentContentTypeWithParameter() {
		// The multipart/message ban applies regardless of parameters and case
		assertThrows(ConditionError.class, () -> executeWithAttachmentContentType("Multipart/Mixed; boundary=x"));
	}

	@Test
	public void testEvaluate_rejectAttachmentContentTypeWithMalformedParameter() {
		// a parameter must be name=value
		assertThrows(ConditionError.class, () -> executeWithAttachmentContentType("text/plain; charset"));
	}

	@Test
	public void testEvaluate_rejectAttachmentContentWithLineBreak() {
		assertThrows(ConditionError.class, () -> runTest(EkycUnknownPropertyFixtures.RESPONSE_ATTACHMENT_CONTENT_WITH_LINE_BREAK));
	}

	@Test
	public void testEvaluate_rejectForbiddenAttachmentContentTypeLowercase() {
		assertThrows(ConditionError.class, () -> executeWithAttachmentContentType("multipart/mixed"));
	}

	@Test
	public void testEvaluate_rejectForbiddenAttachmentContentTypeMixedCase() {
		// Media type names are case-insensitive (RFC 6838, section 4.2)
		assertThrows(ConditionError.class, () -> executeWithAttachmentContentType("Multipart/mixed"));
	}

	@Test
	public void testEvaluate_rejectForbiddenAttachmentContentTypeUppercase() {
		assertThrows(ConditionError.class, () -> executeWithAttachmentContentType("MESSAGE/rfc822"));
	}

	@Test
	public void testEvaluate_allowedAttachmentContentType() {
		assertDoesNotThrow(() -> executeWithAttachmentContentType("image/png"));
	}

	@Test
	public void testEvaluate_rejectAttachmentContentBase64Url() {
		// The attachments spec says "Base64 encoded ... See [RFC4648]"; RFC 4648 section 5 says
		// base64url "should not be regarded as the same as the 'base64' encoding and should not
		// be referred to as only 'base64'", so only the section 4 alphabet (+/) is conformant.
		// The spec-side clarification is tracked in https://github.com/openid/eKYC-IDA/issues/400
		// - if the WG settles on (unpadded) base64url, flip these expectations and the schema
		// patterns for attachment content and digest value.
		assertThrows(ConditionError.class, () -> executeWithAttachmentContent("rB6JGvh-Sxmwo_Yzc9YAyKtau2eWpjIcq0Wta-j7uDs="));
	}

	@Test
	public void testEvaluate_rejectAttachmentContentUnpadded() {
		// RFC 4648 section 3.2: pad characters are a MUST unless the referring spec states
		// otherwise, and the attachments spec does not (see also eKYC-IDA issue 400 above).
		assertThrows(ConditionError.class, () -> executeWithAttachmentContent("aGVsbG8"));
	}

	@Test
	public void testEvaluate_rejectAttachmentContentWithImpossibleBase64Length() {
		// 5 characters can never be a base64 encoding (length % 4 == 1)
		assertThrows(ConditionError.class, () -> executeWithAttachmentContent("aGVsb"));
	}

	@Test
	public void testEvaluate_rejectAttachmentMixingEmbeddedAndExternal() {
		// An attachment is either embedded (content_type/content) or external (url/digest);
		// carrying members of both satisfies neither oneOf branch.
		assertThrows(ConditionError.class, () -> runTest(EkycUnknownPropertyFixtures.RESPONSE_ATTACHMENT_MIXING_EMBEDDED_AND_EXTERNAL));
	}

	@Test
	public void testEvaluate_rejectAttachmentMixingEmbeddedAndExternalWithInvalidContent() {
		// Regression test: without the cross-branch "false" discriminators, strictness
		// stripping let the external branch absorb the embedded members and this payload's
		// invalid base64 content validated cleanly.
		assertThrows(ConditionError.class, () -> runTest(EkycUnknownPropertyFixtures.RESPONSE_ATTACHMENT_MIXING_EMBEDDED_AND_EXTERNAL_WITH_INVALID_CONTENT));
	}

	@Test
	public void testEvaluate_rejectAttachmentContentEmpty() {
		// The base64 pattern alone matches the empty string (zero groups, optional padding);
		// minLength closes that off.
		assertThrows(ConditionError.class, () -> executeWithAttachmentContent(""));
	}

	@Test
	public void testEvaluate_rejectAttachmentDigestValueEmpty() {
		assertThrows(ConditionError.class, () -> runTest("""
			{
			  "claims": {"given_name": "Paula"},
			  "verification": {
			    "trust_framework": "de_aml",
			    "evidence": [{
			      "type": "document",
			      "attachments": [{
			        "url": "https://example.com/a",
			        "digest": {"alg": "sha-256", "value": ""}
			      }]
			    }]
			  }
			}
			"""));
	}

	@Test
	public void testEvaluate_allowedExternalAttachment() {
		assertDoesNotThrow(() -> runTest("""
			{
			  "claims": {"given_name": "Paula"},
			  "verification": {
			    "trust_framework": "de_aml",
			    "evidence": [{
			      "type": "document",
			      "attachments": [{
			        "url": "https://example.com/a",
			        "digest": {"alg": "sha-256", "value": "aGk="}
			      }]
			    }]
			  }
			}
			"""));
	}

	private void executeWithAttachmentContentType(String contentType) {
		executeWithAttachment(contentType, "aGVsbG8=");
	}

	private void executeWithAttachmentContent(String content) {
		executeWithAttachment("image/png", content);
	}

	private void executeWithAttachment(String contentType, String content) {
		runTest("""
			{
			  "claims": {"given_name": "Paula"},
			  "verification": {
			    "trust_framework": "de_aml",
			    "evidence": [{
			      "type": "document",
			      "attachments": [{
			        "content_type": "%s",
			        "content": "%s"
			      }]
			    }]
			  }
			}
			""".formatted(contentType, content));
	}
}
