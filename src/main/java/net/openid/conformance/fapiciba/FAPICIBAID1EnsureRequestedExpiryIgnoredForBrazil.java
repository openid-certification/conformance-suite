package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddRequestedExp1sToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckBackchannelExpiresInDoesNotMatchRequestedExpiry;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-requested-expiry-is-ignored-for-brazil",
	displayName = "FAPI-CIBA-ID1: Ensure requested_expiry is ignored for Brazil",
	summary = "This test makes a Brazil CIBA request with requested_expiry set to one second and checks that this value did not influence expires_in in the backchannel response. If expires_in matches requested_expiry, the test fails.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBAID1EnsureRequestedExpiryIgnoredForBrazil extends AbstractFAPICIBAID1 {

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		callAndStopOnFailure(AddRequestedExp1sToAuthorizationEndpointRequest.class, "BrazilCIBA-6.2.6");
	}

	@Override
	protected void performValidateAuthorizationResponse() {
		super.performValidateAuthorizationResponse();
		callAndContinueOnFailure(CheckBackchannelExpiresInDoesNotMatchRequestedExpiry.class, Condition.ConditionResult.FAILURE, "BrazilCIBA-6.2.6");
	}
}
