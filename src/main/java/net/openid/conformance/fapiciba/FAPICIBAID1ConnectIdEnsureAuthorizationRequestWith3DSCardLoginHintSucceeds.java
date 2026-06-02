package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.SetConnectIdCibaLoginHintToCardPrimaryAccountNumber;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-connectid-ensure-authorization-request-with-3ds-card-login-hint-succeeds",
	displayName = "ConnectID CIBA: Test with a 3DS card primary account number login_hint, the server must authenticate successfully",
	summary = "This test sends a ConnectID CIBA backchannel authentication request with login_hint set to a card primary account number, matching the 3DS login hint shape used by the ConnectID CIBA RP SDK sample. The server must authenticate successfully.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class FAPICIBAID1ConnectIdEnsureAuthorizationRequestWith3DSCardLoginHintSucceeds extends AbstractFAPICIBAID1 {

	@Override
	protected void performProfileAuthorizationEndpointSetup() {
		super.performProfileAuthorizationEndpointSetup();
		callAndStopOnFailure(SetConnectIdCibaLoginHintToCardPrimaryAccountNumber.class,
			"CID-CIBA-4.1.2.1", "CID-CIBA-4.3-1");
	}
}
