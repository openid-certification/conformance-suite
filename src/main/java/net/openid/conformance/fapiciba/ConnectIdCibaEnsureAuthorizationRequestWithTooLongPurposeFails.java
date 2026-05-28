package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.SetRequestObjectBindingMessageToTooLongPurpose;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "connectid-ciba-ensure-authorization-request-with-too-long-purpose-fails",
	displayName = "ConnectID CIBA: binding_message purpose longer than 300 characters, should return invalid_binding_message",
	summary = "This test sends a ConnectID CIBA backchannel authentication request with binding_message longer than the maximum purpose length. The server must return an invalid_binding_message error.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class ConnectIdCibaEnsureAuthorizationRequestWithTooLongPurposeFails extends AbstractConnectIdCibaEnsureInvalidBindingMessageFails {

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(SetRequestObjectBindingMessageToTooLongPurpose.class, "CID-IDA-5.2-10", "CID-PURPOSE-3");
	}
}
