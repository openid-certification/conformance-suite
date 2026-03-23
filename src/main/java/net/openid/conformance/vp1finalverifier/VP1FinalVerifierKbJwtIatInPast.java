package net.openid.conformance.vp1finalverifier;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.as.CreateSdJwtKbCredentialWithIatInPast;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "oid4vp-1final-verifier-kb-jwt-iat-in-past",
	displayName = "OID4VP-1.0-FINAL Verifier: KB-JWT iat 1 year in the past",
	summary = """
		Creates a credential where the iat claim in the Key Binding JWT is set to 1 year \
		in the past. The verifier should reject this presentation as the Key Binding JWT \
		creation time is not within an acceptable window.

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
public class VP1FinalVerifierKbJwtIatInPast extends AbstractVP1FinalVerifierTest {

	@Override
	protected void createSdJwtCredential() {
		callAndStopOnFailure(CreateSdJwtKbCredentialWithIatInPast.class);
	}

	@Override
	protected void validateDirectPostEndpointResponse() {
		callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, ConditionResult.FAILURE, "OID4VP-1FINAL-8.2");
	}
}
