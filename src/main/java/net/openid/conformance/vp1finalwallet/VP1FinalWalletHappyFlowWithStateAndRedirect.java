package net.openid.conformance.vp1finalwallet;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.AddRandomParameterToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-1final-wallet-happy-flow-with-state-and-redirect",
	displayName = "OID4VP-1.0-FINAL: Happy flow test with state parameter and a redirect back to the verifier",
	summary = "Performs the normal flow, but with a 'state', a longer 'nonce', a random authorization endpoint parameter (which must be ignored) and the response_uri response returns a redirect_uri which the wallet must open",
	profile = "OID4VP-1FINAL",
	configurationFields = {
		"server.authorization_endpoint"
	}
)

public class VP1FinalWalletHappyFlowWithStateAndRedirect extends AbstractVP1FinalWalletTest {

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
	protected void populateDirectPostResponse() {
		super.populateDirectPostResponseWithRedirectUri();
	}
}
