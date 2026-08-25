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
	public void testEvaluate_rejectAttachmentContentTypeWithParameters() {
		assertThrows(ConditionError.class, () -> runTest(EkycUnknownPropertyFixtures.RESPONSE_ATTACHMENT_CONTENT_TYPE_WITH_PARAMETERS));
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

	private void executeWithAttachmentContentType(String contentType) {
		runTest("""
			{
			  "claims": {"given_name": "Paula"},
			  "verification": {
			    "trust_framework": "de_aml",
			    "evidence": [{
			      "type": "document",
			      "attachments": [{
			        "content_type": "%s",
			        "content": "aGVsbG8="
			      }]
			    }]
			  }
			}
			""".formatted(contentType));
	}
}
