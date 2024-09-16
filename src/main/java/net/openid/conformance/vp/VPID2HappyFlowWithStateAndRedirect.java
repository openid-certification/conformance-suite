package net.openid.conformance.vp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AddRandomParameterToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-happy-flow-with-state-and-redirect",
	displayName = "OID4VP: Happy flow test with state parameter and a redirect back to the verifier",
	summary = "Performs the normal flow, but with a 'state', a longer 'nonce', a random authorization endpoint parameter (which must be ignored) and the response_uri response returns a redirect_uri which the wallet must open",
	profile = "OID4VP-ID2",
	configurationFields = {
		"client.presentation_definition"
	}
)

public class VPID2HappyFlowWithStateAndRedirect extends AbstractVPServerTest {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.onConfigure(config, baseUrl);
		callAndStopOnFailure(CreateRedirectUri.class);
		// try a longer nonce
		env.putInteger("requested_nonce_length", 32);

		// also use a longer state value than is used by default
		env.putInteger("requested_state_length", 64);

		// FIXME: is response_uri is optional when using the redirect_uri scheme; if so we should omit it in this test: https://github.com/openid/OpenID4VP/issues/93
	}

	@Override
	protected ConditionSequence createAuthorizationRequestSequence() {
		ConditionSequence createAuthorizationRequestSteps = super.createAuthorizationRequestSequence();

		createAuthorizationRequestSteps = createAuthorizationRequestSteps.
			then(condition(AddRandomParameterToAuthorizationEndpointRequest.class));

		return createAuthorizationRequestSteps;
	}

	@Override
	protected void populateDirectPostResponse(JsonObject response) {
		super.populateDirectPostResponseWithRedirectUri(response);
	}
}
