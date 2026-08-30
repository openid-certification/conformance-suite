package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.as.InvalidateNonce;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-invalid-session-transcript",
	displayName = "OID4VP-1.0-FINAL Verifier: Invalid session transcript",
	summary = """
		Presents an otherwise valid mdoc whose device authentication was computed over an \
		incorrect session transcript: the wallet builds the OpenID4VPHandover using an \
		incorrect nonce (the verifier's nonce with 'INVALID' appended) instead of the nonce \
		from the authorization request, and signs (or MACs) the DeviceAuth over the resulting \
		session transcript. Everything else about the credential is valid. The verifier must \
		reject the presentation: recomputing the session transcript with the nonce it actually \
		sent, the mdoc's device signature or MAC does not verify.

		This test is only applicable for the ISO mdoc credential format.

		The conformance suite acts as a mock web wallet. You must configure your verifier to use \
		the authorization endpoint url below instead of 'openid4vp://' and then start the flow in \
		your verifier as normal.

		On a 4xx response the test passes immediately; on a success response a screenshot of the \
		verifier's error must be uploaded and the test finishes as REVIEW.
		""",
	profile = "OID4VP-1FINAL",
	configurationFields = {
		"credential.signing_jwk"
	}
)
@VariantNotApplicable(parameter = VP1FinalVerifierCredentialFormat.class, values = {"sd_jwt_vc"})
public class VP1FinalVerifierInvalidSessionTranscript extends AbstractVP1FinalVerifierNegativeTest {

	@Override
	protected void createIsoMdlSessionTranscript() {
		callAndStopOnFailure(InvalidateNonce.class);
		super.createIsoMdlSessionTranscript();
	}
}
