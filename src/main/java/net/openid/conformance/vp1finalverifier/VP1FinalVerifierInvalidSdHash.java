package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.as.CreateSdJwtKbCredentialWithInvalidSdHash;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-invalid-sd-hash",
	displayName = "OID4VP-1.0-FINAL Verifier: Invalid sd_hash in KB-JWT",
	summary = """
		Creates a credential where the sd_hash claim in the Key Binding JWT does not match \
		the hash of the presented SD-JWT. The verifier must reject this credential.

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
public class VP1FinalVerifierInvalidSdHash extends AbstractVP1FinalVerifierNegativeTest {

	@Override
	protected void createSdJwtCredential() {
		callAndStopOnFailure(CreateSdJwtKbCredentialWithInvalidSdHash.class);
	}
}
