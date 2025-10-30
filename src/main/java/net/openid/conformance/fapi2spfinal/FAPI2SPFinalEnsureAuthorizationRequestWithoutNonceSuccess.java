package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIOpenIDConnect;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-authorization-request-without-nonce-success",
	displayName = "FAPI2-Security-Profile-Final: ensure authorization endpoint request without noce success",
	summary = "This test makes an authentication request that does not include 'nonce'. nonce is an optional parameter for response_type=code, so the authorization server must successfully authenticate and must not return a nonce.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = FAPIOpenIDConnect.class, values = { "plain_oauth" })
public class FAPI2SPFinalEnsureAuthorizationRequestWithoutNonceSuccess extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		return super.makeCreateAuthorizationRequestSteps()
				.skip(CreateRandomNonceValue.class, "NOT creating nonce")
				.skip(AddNonceToAuthorizationEndpointRequest.class,
						"NOT adding nonce to request object");
	}

}
