package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.RemoveJtiFromRequestObject;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-request-object-missing-jti-fails",
	displayName = "FAPI-CIBA-ID1: Missing 'jti' value in request object, should return an error",
	summary = "This test should return an error that the 'jti' value in request object from back channel authentication endpoint request is missing",
	profile = "FAPI-CIBA-ID1"
)
public class FAPICIBAID1EnsureRequestObjectMissingJtiFails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();

		callAndStopOnFailure(RemoveJtiFromRequestObject.class, "CIBA-7.1.1");
	}
}
