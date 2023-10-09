package net.openid.conformance.openid;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddIdTokenSigningAlgNoneToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CheckIdTokenSignatureAlgorithm;
import net.openid.conformance.condition.client.OIDCCCheckIdTokenSigningAlgValuesSupportedAlgNone;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientRegistration;
import net.openid.conformance.variant.ResponseType;
import net.openid.conformance.variant.VariantNotApplicable;

// Corresponds to OPIdToken-none
@PublishTestModule(
	testName = "oidcc-idtoken-unsigned",
	displayName = "OIDCC: check ID token with no signature",
	summary = "This test requests an ID token signed with \"none\".",
	profile = "OIDCC"
)
@VariantNotApplicable(parameter = ClientRegistration.class, values = { "static_client" })
@VariantNotApplicable(parameter = ResponseType.class, values = { "code id_token", "code id_token token", "id_token", "id_token token" })
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

	@Override
	protected void skipTestIfSigningAlgorithmNotSupported() {

		if (serverSupportsDiscovery()) {
			callAndContinueOnFailure(OIDCCCheckIdTokenSigningAlgValuesSupportedAlgNone.class, Condition.ConditionResult.INFO);

			Boolean idTokenSigningAlgSupportedFlag = env.getBoolean("id_token_signing_alg_not_supported_flag");
			if (idTokenSigningAlgSupportedFlag != null && idTokenSigningAlgSupportedFlag) {
				fireTestSkipped("The discovery endpoint 'id_token_signing_alg_values_supported' doesn't support 'none' algorithm; this cannot be tested (which is acceptable for certification, servers are not required to support 'none'");
			}
		}
	}
}
