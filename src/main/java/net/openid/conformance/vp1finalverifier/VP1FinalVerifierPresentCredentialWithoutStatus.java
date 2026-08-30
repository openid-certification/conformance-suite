package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-present-credential-without-status",
	displayName = "OID4VP-1.0-FINAL Verifier: Present a credential without revocation information",
	summary = """
		Presents a credential that carries no revocation information at all: for the SD-JWT VC \
		credential format there is no 'status' claim, and for the ISO mdoc credential format the \
		Mobile Security Object has no status element. Both are optional (draft-ietf-oauth-status-list \
		section 6.2; ISO/IEC 18013-5 12.3.6.2 "An MSO may contain the Status structure"), so the \
		verifier must accept the presentation: a verifier that rejects an otherwise valid \
		credential because it carries no revocation information fails this test.

		Other than the missing status reference the credential and flow are identical to the happy \
		flow. The conformance suite acts as a mock web wallet. You must configure your verifier to \
		use the authorization endpoint url below instead of 'openid4vp://' and then start the flow \
		in your verifier as normal.

		The verifier must return a success response at the response_uri, and a screenshot showing \
		it reporting successful verification must then be uploaded; the test finishes as REVIEW.
		""",
	profile = "OID4VP-1FINAL",
	configurationFields = {
		"credential.signing_jwk"
	}
)
public class VP1FinalVerifierPresentCredentialWithoutStatus extends AbstractVP1FinalVerifierTest {

	@Override
	protected void createRevocationListReference() {
		eventLog.log(getName(), "Not allocating a status list reference: the presented credential "
			+ "will carry no revocation information, which the specifications permit.");
	}
}
