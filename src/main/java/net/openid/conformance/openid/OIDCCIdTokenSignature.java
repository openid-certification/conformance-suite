package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.EnsureIdTokenContainsKid;
import net.openid.conformance.condition.client.EnsureIdTokenSignatureIsRS256;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.VariantNotApplicable;

// Corresponds to OPIdToken-kid and OP-IDToken-ignature
@PublishTestModule(
	testName = "oidcc-idtoken-signature",
	displayName = "OIDCC: check ID token with default signature",
	summary = "This test requests an ID token without specifying an algorithm (which should default to RS256).",
	profile = "OIDCC"
)
@VariantNotApplicable(parameter = ClientRegistration.class, values = { "static_client" })
public class OIDCCIdTokenSignature extends AbstractOIDCCServerTest {

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		// Don't specify an ID token signing algorithm
	}

	@Override
	protected void performIdTokenValidation() {
		// OP-IDToken-kid
		// OIDCC-10.1 seems to only require a KID if the server has multiple JWKs,
		// but we'll replicate the python test here and require it always.
		callAndContinueOnFailure(EnsureIdTokenContainsKid.class, Condition.ConditionResult.FAILURE, "OIDCC-10.1");

		callAndContinueOnFailure(EnsureIdTokenSignatureIsRS256.class, ConditionResult.FAILURE, "OIDCC-3.1.3.7");

		super.performIdTokenValidation();
	}

}
