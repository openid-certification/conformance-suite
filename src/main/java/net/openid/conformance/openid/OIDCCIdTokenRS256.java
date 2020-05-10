package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddIdTokenSigningAlgRS256ToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CheckIdTokenSignatureAlgorithm;
import net.openid.conformance.condition.client.EnsureIdTokenContainsKid;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantNotApplicable;

// Corresponds to OPIdToken-kid and OP-IDToken-RS256
@PublishTestModule(
	testName = "oidcc-idtoken-rs256",
	displayName = "OIDCC: check ID token with RS256 signature",
	summary = "This test requests an ID token signed with RS256.",
	profile = "OIDCC"
)
@VariantNotApplicable(parameter = ClientRegistration.class, values = { "static_client" })
public class OIDCCIdTokenRS256 extends AbstractOIDCCServerTest {

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(AddIdTokenSigningAlgRS256ToDynamicRegistrationRequest.class);
	}

	@Override
	protected void performIdTokenValidation() {
		// OP-IDToken-kid
		// OIDCC-10.1 seems to only require a KID if the server has multiple JWKs,
		// but we'll replicate the python test here and require it always.
		callAndContinueOnFailure(EnsureIdTokenContainsKid.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");

		// OP-IDToken-RS256
		callAndContinueOnFailure(CheckIdTokenSignatureAlgorithm.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.7");

		super.performIdTokenValidation();
	}

}
