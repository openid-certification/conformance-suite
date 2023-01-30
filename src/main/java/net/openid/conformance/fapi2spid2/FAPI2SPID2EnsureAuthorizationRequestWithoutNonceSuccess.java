package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIOpenIDConnect;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-ensure-authorization-request-without-nonce-success",
	displayName = "FAPI2-Security-Profile-ID2: ensure authorization endpoint request without noce success",
	summary = "This test makes an authentication request that does not include 'nonce'. nonce is an optional parameter for response_type=code, so the authorization server must successfully authenticate and must not return a nonce.",
	profile = "FAPI2-Security-Profile-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = FAPIOpenIDConnect.class, values = { "plain_oauth" })
public class FAPI2SPID2EnsureAuthorizationRequestWithoutNonceSuccess extends AbstractFAPI2SPID2ServerTestModule {

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		return super.makeCreateAuthorizationRequestSteps()
				.skip(CreateRandomNonceValue.class, "NOT creating nonce")
				.skip(AddNonceToAuthorizationEndpointRequest.class,
						"NOT adding nonce to request object");
	}

}
