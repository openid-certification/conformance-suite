package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-connectid-userinfo-response-mismatched-fapi-interaction-id-test",
	displayName = "ConnectID CIBA: RP test - userinfo response has a mismatched x-fapi-interaction-id",
	summary = "The userinfo response contains a valid UUID x-fapi-interaction-id that differs from the request value. The client must reject the response and stop interacting with the server.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {
	"plain_fapi", "openbanking_uk", "openbanking_brazil"
})
public class FAPICIBAClientConnectIdUserInfoResponseMismatchedFAPIInteractionIdTest
	extends AbstractFAPICIBAClientInvalidUserInfoEndpointInteractionIdTest {

	@Override
	protected void customizeUserInfoEndpointResponseHeaders() {
		callAndStopOnFailure(SetDifferentFAPIInteractionIdInUserInfoEndpointResponse.class, "CID-SP-4.4-1");
	}
}
