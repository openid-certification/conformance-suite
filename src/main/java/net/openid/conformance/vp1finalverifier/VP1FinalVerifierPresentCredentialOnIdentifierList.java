package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.as.CreateMDocGeneratedNonce;
import net.openid.conformance.condition.as.CreateMdocCredential;
import net.openid.conformance.condition.as.CreateRevokedIdentifierListReference;
import net.openid.conformance.condition.as.VP1FinalGenerateIdentifierListToken;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-present-credential-on-identifier-list",
	displayName = "OID4VP-1.0-FINAL Verifier: Present a credential revoked via an identifier list",
	summary = """
		Presents an mdoc whose Mobile Security Object carries the identifier_list element of \
		ISO/IEC 18013-5 12.3.6.2 - the second of the two MSO revocation mechanisms, alongside the \
		status list one - naming an identifier that appears in the identifier list this test \
		serves, i.e. the MSO is revoked. The verifier must reject the presentation.

		The identifier list is served as the CWT of 12.3.6.4: the envelope 12.3.6.3 defines for \
		both mechanisms, with the 'application/identifierlist+cwt' type, no StatusList claim and \
		the IdentifierList structure at CWT claim key 65530. It is signed by a certificate issued \
		by the same IACA root as the mdoc's document signer certificate, which is what 12.3.6.2 \
		requires when the MSO's status element carries no Certificate element - so a verifier \
		configured with the trust anchors the other tests in this plan need can verify it as well.

		The conformance suite acts as a mock web wallet. You must configure your verifier to use \
		the authorization endpoint url below instead of 'openid4vp://' and then start the flow in \
		your verifier as normal.

		On a 4xx response the test passes immediately; on a success response a screenshot of the \
		verifier's error must be uploaded and the test finishes as REVIEW. If the verifier does \
		not fetch the identifier list at all, it cannot have detected the revocation.
		""",
	profile = "OID4VP-1FINAL",
	configurationFields = {
		"credential.signing_jwk"
	}
)
// The identifier list is an MSO revocation mechanism; SD-JWT VC has no equivalent.
@VariantNotApplicable(parameter = VP1FinalVerifierCredentialFormat.class, values = { "sd_jwt_vc" })
public class VP1FinalVerifierPresentCredentialOnIdentifierList extends AbstractVP1FinalVerifierNegativeTest {

	/**
	 * Replaces the base class's status list flow wholesale rather than extending it: the two
	 * mechanisms are alternatives, and ISO/IEC 18013-5 12.3.6.2 gives the MSO's status element
	 * one of them, not both.
	 */
	@Override
	protected void createCredential() {
		// must run before the credential is created; the credential carries the reference
		callAndStopOnFailure(CreateRevokedIdentifierListReference.class, "ISO18013-5-12.3.6.2");
		callAndStopOnFailure(CreateMDocGeneratedNonce.class);
		createIsoMdlSessionTranscript();
		callAndStopOnFailure(CreateMdocCredential.class);
		// generate the identifier list now so it is ready to serve however quickly the verifier
		// fetches it - see AbstractVP1FinalVerifierTest.handleIdentifierListRequest
		callAndStopOnFailure(VP1FinalGenerateIdentifierListToken.class, "ISO18013-5-12.3.6.3",
			"ISO18013-5-12.3.6.4");
	}
}
