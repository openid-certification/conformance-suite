package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.InvalidateSdJwtKbSignature;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-invalid-kb-jwt-signature",
	displayName = "OID4VP-1.0-FINAL Verifier: Invalid KB-JWT signature",
	summary = """
		Creates a credential where the Key Binding JWT signature has been corrupted. \
		The verifier must reject this credential because the KB-JWT signature cannot be \
		verified against the public key in the credential's cnf claim.

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
public class VP1FinalVerifierInvalidKbJwtSignature extends AbstractVP1FinalVerifierTest {

	@Override
	protected void createSdJwtCredential() {
		super.createSdJwtCredential();
		callAndStopOnFailure(InvalidateSdJwtKbSignature.class);
	}

	@Override
	protected void validateDirectPostEndpointResponse() {
		callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, ConditionResult.FAILURE, "OID4VP-1FINAL-8.2");
	}
}
