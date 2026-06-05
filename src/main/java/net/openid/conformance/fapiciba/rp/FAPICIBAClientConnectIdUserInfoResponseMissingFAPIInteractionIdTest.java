package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-connectid-userinfo-response-missing-fapi-interaction-id-test",
	displayName = "ConnectID CIBA: RP test - userinfo response is missing x-fapi-interaction-id",
	summary = "The userinfo response omits x-fapi-interaction-id. The client must reject the response and stop interacting with the server.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {
	"plain_fapi", "openbanking_uk", "openbanking_brazil"
})
public class FAPICIBAClientConnectIdUserInfoResponseMissingFAPIInteractionIdTest
	extends AbstractFAPICIBAClientInvalidUserInfoEndpointInteractionIdTest {

	@Override
	protected void customizeUserInfoEndpointResponseHeaders() {
		callAndStopOnFailure(RemoveFAPIInteractionIdFromUserInfoEndpointResponse.class, "CID-SP-4.4-1");
	}
}
