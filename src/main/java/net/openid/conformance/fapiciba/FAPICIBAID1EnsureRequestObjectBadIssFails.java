package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.AddIssForSecondClientToRequestObject;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-request-object-bad-iss-fails",
	displayName = "FAPI-CIBA-ID1: Bad 'iss' value in request object, should return an error",
	summary = "This test should return an error that the 'iss' value in request object from back channel authentication endpoint request is incorrect (a valid client_id, but not for the client in use)",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPICIBAID1EnsureRequestObjectBadIssFails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void configClient() {
		super.configClient();

		// This test requires the second client.
		setupClient2();
	}


	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(AddIssForSecondClientToRequestObject.class, "CIBA-7.1.1");
	}

	@Override
	public void cleanup() {
		unregisterClient1();

		unregisterClient2();
	}

}
