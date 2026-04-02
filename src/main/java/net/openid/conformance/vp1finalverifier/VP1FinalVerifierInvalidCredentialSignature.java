package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.as.InvalidateSdJwtCredentialSignature;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-invalid-credential-signature",
	displayName = "OID4VP-1.0-FINAL Verifier: Invalid credential signature",
	summary = """
		Creates a credential where the issuer's SD-JWT signature has been corrupted. \
		The verifier must reject this credential because the issuer signature cannot be verified.

		This test is only applicable for the SD-JWT VC credential format.

		The conformance suite acts as a mock web wallet. You must configure your verifier to use \
		the authorization endpoint url below instead of 'openid4vp://' and then start the flow in \
		your verifier as normal.
		""",
	profile = "OID4VP-1FINAL",
	configurationFields = {
		"credential.signing_jwk"
	}
)
@VariantNotApplicable(parameter = VP1FinalVerifierCredentialFormat.class, values = {"iso_mdl"})
public class VP1FinalVerifierInvalidCredentialSignature extends AbstractVP1FinalVerifierNegativeTest {

	@Override
	protected void createSdJwtCredential() {
		super.createSdJwtCredential();
		callAndStopOnFailure(InvalidateSdJwtCredentialSignature.class);
	}
}
