package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.SetRequestObjectBindingMessageToTooShortPurpose;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-connectid-ensure-authorization-request-with-too-short-purpose-fails",
	displayName = "ConnectID CIBA: binding_message purpose shorter than 3 characters, should return invalid_binding_message",
	summary = "This test sends a ConnectID CIBA backchannel authentication request with binding_message shorter than the minimum purpose length. The server must return an invalid_binding_message error.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class FAPICIBAID1ConnectIdEnsureAuthorizationRequestWithTooShortPurposeFails extends AbstractConnectIdCibaEnsureInvalidBindingMessageFails {

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(SetRequestObjectBindingMessageToTooShortPurpose.class, "CID-IDA-5.2-10", "CID-PURPOSE-3");
	}
}
