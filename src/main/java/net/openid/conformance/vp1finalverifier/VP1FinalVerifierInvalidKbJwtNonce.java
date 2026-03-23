package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.InvalidateNonce;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-invalid-kb-jwt-nonce",
	displayName = "OID4VP-1.0-FINAL Verifier: Invalid nonce in KB-JWT",
	summary = """
		Creates a credential where the nonce claim in the Key Binding JWT does not match \
		the nonce from the authorization request. The verifier must reject this presentation.

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
public class VP1FinalVerifierInvalidKbJwtNonce extends AbstractVP1FinalVerifierTest {

	@Override
	protected void createSdJwtCredential() {
		callAndStopOnFailure(InvalidateNonce.class);
		super.createSdJwtCredential();
	}

	@Override
	protected void validateDirectPostEndpointResponse() {
		callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, ConditionResult.FAILURE, "OID4VP-1FINAL-8.2");
	}
}
