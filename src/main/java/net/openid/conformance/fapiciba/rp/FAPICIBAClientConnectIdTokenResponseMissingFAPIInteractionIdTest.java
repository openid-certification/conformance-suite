package net.openid.conformance.fapiciba.rp;

import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-client-connectid-token-response-missing-fapi-interaction-id-test",
	displayName = "ConnectID CIBA: RP test - token response is missing x-fapi-interaction-id",
	summary = "The token endpoint response omits x-fapi-interaction-id. The client must reject the response and stop interacting with the server.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {
	"plain_fapi", "openbanking_uk", "openbanking_brazil"
})
public class FAPICIBAClientConnectIdTokenResponseMissingFAPIInteractionIdTest
	extends AbstractFAPICIBAClientInvalidTokenEndpointInteractionIdTest {

	@Override
	protected void customizeTokenEndpointResponseHeaders() {
		callAndStopOnFailure(RemoveFAPIInteractionIdFromTokenEndpointResponse.class, "CID-SP-4.2-12");
	}
}
