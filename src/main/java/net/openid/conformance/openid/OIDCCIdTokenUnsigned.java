package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckIdTokenSignatureAlgorithm;
import net.openid.conformance.condition.client.AddIdTokenSigningAlgNoneToDynamicRegistrationRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

// Corresponds to OPIdToken-none
@PublishTestModule(
	testName = "oidcc-idtoken-unsigned",
	displayName = "OIDCC: check ID token with no signature",
	summary = "This test requests an ID token signed with \"none\".",
	profile = "OIDCC",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client2.scope"
	}
)
@VariantNotApplicable(parameter = ClientRegistration.class, values = { "static_client" })
@VariantNotApplicable(parameter = ResponseType.class, values = { "code", "code token" })
public class OIDCCIdTokenUnsigned extends AbstractOIDCCServerTest {

	@Override
	protected void createDynamicClientRegistrationRequest() {
		super.createDynamicClientRegistrationRequest();
		callAndStopOnFailure(AddIdTokenSigningAlgNoneToDynamicRegistrationRequest.class);
	}

	@Override
	protected void performIdTokenValidation() {
		callAndContinueOnFailure(CheckIdTokenSignatureAlgorithm.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.7");
	}

}
