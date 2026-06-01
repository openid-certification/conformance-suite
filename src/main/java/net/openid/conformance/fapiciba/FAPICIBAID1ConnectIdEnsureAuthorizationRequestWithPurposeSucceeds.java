package net.openid.conformance.fapiciba;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-connectid-ensure-authorization-request-with-purpose-succeeds",
	displayName = "ConnectID CIBA: Test with binding_message as a purpose statement, the server must authenticate successfully",
	summary = "This test sends a ConnectID CIBA backchannel authentication request with binding_message containing an ASCII human-readable purpose statement. The server must authenticate successfully.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "openbanking_brazil"})
public class FAPICIBAID1ConnectIdEnsureAuthorizationRequestWithPurposeSucceeds extends AbstractFAPICIBAID1 {
}
