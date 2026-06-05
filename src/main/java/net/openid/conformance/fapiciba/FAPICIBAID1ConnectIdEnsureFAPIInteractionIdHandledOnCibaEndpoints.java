package net.openid.conformance.fapiciba;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-connectid-ensure-fapi-interaction-id-handled-on-ciba-endpoints",
	displayName = "ConnectID CIBA: x-fapi-interaction-id must be handled on CIBA endpoints",
	summary = "This test sends a unique UUID x-fapi-interaction-id on the backchannel authentication, token, and userinfo requests. The server must return the same value in each corresponding response.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {
	"plain_fapi", "openbanking_uk", "openbanking_brazil"
})
public class FAPICIBAID1ConnectIdEnsureFAPIInteractionIdHandledOnCibaEndpoints extends AbstractFAPICIBAID1 {
}
