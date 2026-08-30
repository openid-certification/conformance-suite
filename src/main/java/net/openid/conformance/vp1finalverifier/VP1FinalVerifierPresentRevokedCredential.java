package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.as.CreateRevokedStatusListReference;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-present-revoked-credential",
	displayName = "OID4VP-1.0-FINAL Verifier: Present a revoked credential",
	summary = """
		Presents a credential that references a Token Status List, served by this test, that marks \
		the credential as revoked. The verifier must reject the presentation.

		For the SD-JWT VC credential format the credential carries a 'status.status_list' claim and \
		the status list is served as a Status List Token in JWT format, signed with the same key \
		(and certificate chain) as the credential itself. For the ISO mdoc credential format the MSO \
		carries the status element of ISO/IEC 18013-5 12.3.6.2 and the status list is served as a \
		Status List Token in CWT format as 12.3.6.3 requires, signed by a certificate issued by the \
		same IACA root as the mdoc's document signer certificate - so a verifier configured with the \
		trust anchors the other tests in this plan need can verify the status list as well.

		The conformance suite acts as a mock web wallet. You must configure your verifier to use \
		the authorization endpoint url below instead of 'openid4vp://' and then start the flow in \
		your verifier as normal.

		On a 4xx response the test passes immediately; on a success response a screenshot of the \
		verifier's error must be uploaded and the test finishes as REVIEW. If the verifier does not \
		fetch the status list at all, it cannot have detected the revocation.
		""",
	profile = "OID4VP-1FINAL",
	configurationFields = {
		"credential.signing_jwk"
	}
)
public class VP1FinalVerifierPresentRevokedCredential extends AbstractVP1FinalVerifierNegativeTest {

	/**
	 * The reference allocation, status list token generation and serving all live in
	 * {@link AbstractVP1FinalVerifierTest}, which gives the happy flows a valid index; this test
	 * only swaps the allocated index for one the served status list marks as revoked.
	 */
	@Override
	protected void createStatusListReference() {
		callAndStopOnFailure(CreateRevokedStatusListReference.class, "OTSL-6.2", "ISO18013-5-12.3.6.2");
	}
}
