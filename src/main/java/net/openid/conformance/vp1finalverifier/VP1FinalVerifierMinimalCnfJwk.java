package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.as.CreateSdJwtKbCredentialWithMinimalCnf;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-minimal-cnf-jwk",
	displayName = "OID4VP-1.0-FINAL Verifier: Credential with minimal cnf.jwk",
	summary = """
		Creates a credential where the cnf.jwk contains only the required fields \
		for the key type (kty, crv, x, y for EC). Optional JWK metadata (kid, use, \
		alg, key_ops, x5c, x5t, x5t#S256) is stripped. The verifier must still \
		accept this credential.

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
public class VP1FinalVerifierMinimalCnfJwk extends AbstractVP1FinalVerifierTest {

	@Override
	protected void createSdJwtCredential() {
		callAndStopOnFailure(CreateSdJwtKbCredentialWithMinimalCnf.class);
	}
}
