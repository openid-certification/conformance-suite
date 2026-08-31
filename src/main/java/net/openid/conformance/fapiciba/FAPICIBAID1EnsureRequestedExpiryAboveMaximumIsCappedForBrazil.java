package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.client.AddRequestedExp86401sToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-requested-expiry-above-maximum-is-capped-for-brazil",
	displayName = "FAPI-CIBA-ID1: Ensure requested_expiry is capped for Brazil",
	summary = "This test requests an authentication lifetime one second above the Open Finance Brasil data-consent maximum and requires expires_in to be capped at 86400 seconds.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class,
	values = {"plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBAID1EnsureRequestedExpiryAboveMaximumIsCappedForBrazil
	extends AbstractFAPICIBAID1 {

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		callAndStopOnFailure(AddRequestedExp86401sToAuthorizationEndpointRequest.class,
			"BrazilCIBA-6.2.6");
	}
}
