package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.client.ReverseScopeOrderInAuthorizationEndpointRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIOpenIDConnect;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-other-scope-order-succeeds",
	displayName = "FAPI2-Security-Profile-Final: ensure other scope order succeeds",
	summary = "This test makes a FAPI authorization request but with the order of the entries in the 'scope' reversed, which must succeed. As per RFC6749 section 3.3, 'If the value contains multiple space-delimited strings, their order does not matter'. The reason for this test is that some OAuth clients process scopes in a way that the order they are sent to the server is not under the control of a developer using that client, and as per the spec such requests must still be accepted.",
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
// ConnectID only requires the openid scope to be supported, but this test requires at least two scopes
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "connectid_au" })
// Plain oauth may not have the mulptiple scopes required for this test.
@VariantNotApplicable(parameter = FAPIOpenIDConnect.class, values = "plain_oauth")
public class FAPI2SPFinalEnsureOtherScopeOrderSucceeds extends AbstractFAPI2SPFinalServerTestModule {

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		return super.makeCreateAuthorizationRequestSteps()
			.then(condition(ReverseScopeOrderInAuthorizationEndpointRequest.class).requirement("RFC6749-3.3"));
	}
}
