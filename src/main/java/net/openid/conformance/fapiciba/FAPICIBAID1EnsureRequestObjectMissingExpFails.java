package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.RemoveExpFromRequestObject;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-request-object-missing-exp-fails",
	displayName = "FAPI-CIBA-ID1: Missing 'exp' value in request object, should return an error",
	summary = "This test should return an error that the 'exp' value in request object from back channel authentication endpoint request is missing",
	profile = "FAPI-CIBA-ID1"
)
public class FAPICIBAID1EnsureRequestObjectMissingExpFails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(RemoveExpFromRequestObject.class, "CIBA-7.1.1");
	}

}
