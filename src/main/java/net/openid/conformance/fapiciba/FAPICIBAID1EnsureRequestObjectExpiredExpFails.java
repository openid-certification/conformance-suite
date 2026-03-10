package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.AddExpiredExpToRequestObject;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-request-object-expired-exp-fails",
	displayName = "FAPI-CIBA-ID1: 'exp' value in request object already expired, should return an error",
	summary = "This test should return an error that the 'exp' value in request object from back channel authentication endpoint request already expired",
	profile = "FAPI-CIBA-ID1"
)
public class FAPICIBAID1EnsureRequestObjectExpiredExpFails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(AddExpiredExpToRequestObject.class, "CIBA-7.1.1");
	}
}
