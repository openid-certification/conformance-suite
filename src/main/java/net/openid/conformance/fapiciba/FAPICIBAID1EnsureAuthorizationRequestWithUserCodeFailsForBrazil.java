package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddUserCodeToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-authorization-request-with-user-code-fails-for-brazil",
	displayName = "FAPI-CIBA-ID1: Brazil CIBA request with user_code should return invalid_request",
	summary = "This test sends a Brazil CIBA backchannel authentication request containing user_code. The server must reject the request with an invalid_request error.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBAID1EnsureAuthorizationRequestWithUserCodeFailsForBrazil extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		callAndStopOnFailure(AddUserCodeToAuthorizationEndpointRequest.class, "BrazilCIBA-6.2.4");
	}

	@Override
	protected void checkErrorFromBackchannelAuthorizationRequestResponse() {
		callAndContinueOnFailure(CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidRequest.class,
			Condition.ConditionResult.FAILURE, "BrazilCIBA-6.2.4", "CIBA-13");
	}
}
