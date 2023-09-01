package net.openid.conformance.vp;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.client.CreateRandomCodeVerifier;
import net.openid.conformance.condition.client.CreateRedirectUri;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oid4vp-happy-flow-with-state-and-redirect",
	displayName = "OID4VP: Unsigned request_uri",
	summary = "Performs the normal flow, but with a 'state' and the response_uri response includes redirect_uri which the wallet must open",
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
	}

	@Override
	protected void populateDirectPostResponse(JsonObject response) {
		callAndStopOnFailure(CreateRandomCodeVerifier.class);
		response.addProperty("redirect_uri", env.getString("redirect_uri") + "#" + env.getString("code_verifier"));

		eventLog.log(getName(), "The response_uri is returning 'redirect_uri', so the wallet should send the user to that redirect_uri next");
		setStatus(Status.WAITING);
	}
}
