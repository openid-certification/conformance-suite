package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.SetRequestObjectHintToIdTokenHint;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-connectid-ensure-authorization-request-with-id-token-hint-fails",
	displayName = "ConnectID CIBA: id_token_hint instead of login_hint should return an error",
	summary = "This test sends a ConnectID CIBA backchannel authentication request using id_token_hint instead of the ConnectID profile-defined login_hint value. The server must return an invalid_request error.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class FAPICIBAID1ConnectIdEnsureAuthorizationRequestWithIdTokenHintFails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(SetRequestObjectHintToIdTokenHint.class, "CID-CIBA-4.3-1");
	}
}
