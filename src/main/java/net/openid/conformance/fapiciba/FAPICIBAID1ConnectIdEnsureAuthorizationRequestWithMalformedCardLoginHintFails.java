package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.SetRequestObjectLoginHintToMalformedCardPrimaryAccountNumber;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-connectid-ensure-authorization-request-with-malformed-card-login-hint-fails",
	displayName = "ConnectID CIBA: malformed card primary account number login_hint should return an error",
	summary = "This test sends a ConnectID CIBA backchannel authentication request with a malformed card primary account number in login_hint. The server must return an invalid_request error.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class FAPICIBAID1ConnectIdEnsureAuthorizationRequestWithMalformedCardLoginHintFails extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(SetRequestObjectLoginHintToMalformedCardPrimaryAccountNumber.class,
			"CID-CIBA-4.1.2.1", "CID-CIBA-4.1.3.1", "CID-CIBA-4.3.1");
	}
}
