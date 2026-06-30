package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.RemoveTxnClaimRequestFromAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-connectid-ensure-txn-returned-when-not-requested",
	displayName = "ConnectID CIBA: txn must be returned in the ID Token when not requested",
	summary = "This test sends a ConnectID CIBA backchannel authentication request without requesting the txn claim. The server must still return a non-empty txn claim in the ID Token.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {
	"plain_fapi", "openbanking_uk", "openbanking_brazil"
})
public class FAPICIBAID1ConnectIdEnsureTxnReturnedWhenNotRequested extends AbstractFAPICIBAID1 {

	@Override
	protected void performProfileAuthorizationEndpointSetup() {
		super.performProfileAuthorizationEndpointSetup();
		callAndStopOnFailure(RemoveTxnClaimRequestFromAuthorizationEndpointRequest.class,
			"CID-IDA-5.1-6");
	}
}
