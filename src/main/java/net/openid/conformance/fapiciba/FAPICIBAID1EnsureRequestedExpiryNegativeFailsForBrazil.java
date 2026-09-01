package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddRequestedExpNegative1sToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-requested-expiry-negative-fails-for-brazil",
	displayName = "FAPI-CIBA-ID1: Brazil CIBA request with negative requested_expiry should return invalid_request",
	summary = "This test sends requested_expiry with the negative value -1. The authorization server must reject the invalid parameter value with invalid_request.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class,
	values = {"plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBAID1EnsureRequestedExpiryNegativeFailsForBrazil
	extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		callAndStopOnFailure(AddRequestedExpNegative1sToAuthorizationEndpointRequest.class,
			"CIBA-7.1", "BrazilCIBA-6.2.6");
	}

	@Override
	protected void checkErrorFromBackchannelAuthorizationRequestResponse() {
		callAndContinueOnFailure(
			CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest.class,
			Condition.ConditionResult.FAILURE,
			"CIBA-7.2", "CIBA-13", "BrazilCIBA-6.2.6");
	}
}
