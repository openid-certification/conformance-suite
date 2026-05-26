package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.RemoveBindingMessageFromRequestObject;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "connectid-ciba-ensure-authorization-request-without-binding-message-fails",
	displayName = "ConnectID CIBA: Missing binding_message in request object, should return an error",
	summary = "This test sends a ConnectID CIBA backchannel authentication request with no binding_message in the signed request object. The server must return an invalid_request error because binding_message is mandatory.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class ConnectIdCibaEnsureAuthorizationRequestWithoutBindingMessageFails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(RemoveBindingMessageFromRequestObject.class, "CID-CIBA-4.1.1.1", "CID-CIBA-4.1.2.1", "CID-CIBA-4.1.3.1");
	}
}
