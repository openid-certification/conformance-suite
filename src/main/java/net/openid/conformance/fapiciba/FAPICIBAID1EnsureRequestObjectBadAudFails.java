package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.AddBadAudToRequestObject;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-request-object-bad-aud-fails",
	displayName = "FAPI-CIBA-ID1: Bad aud value in request object, should return an error",
	summary = "This test should return an error that the aud value in request object from back channel authentication endpoint request is incorrect (a valid url, but not for the server)",
	profile = "FAPI-CIBA-ID1"
)
public class FAPICIBAID1EnsureRequestObjectBadAudFails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(AddBadAudToRequestObject.class, "CIBA-7.1.1");
	}

}
