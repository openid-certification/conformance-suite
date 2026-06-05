package net.openid.conformance.fapiciba;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-connectid-ensure-token-response-and-id-token-claims-valid",
	displayName = "ConnectID CIBA: Token response and ID Token claims must meet ConnectID requirements",
	summary = "This test completes a ConnectID CIBA flow and verifies the token response access token lifetime and x-fapi-interaction-id. It also verifies that the ID Token contains a non-empty txn, excludes acr, returns the supported requested identity claims, and contains valid verified claims with the au_connectid trust framework.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {
	"plain_fapi", "openbanking_uk", "openbanking_brazil"
})
public class FAPICIBAID1ConnectIdEnsureTokenResponseAndIdTokenClaimsValid extends AbstractFAPICIBAID1 {
}
