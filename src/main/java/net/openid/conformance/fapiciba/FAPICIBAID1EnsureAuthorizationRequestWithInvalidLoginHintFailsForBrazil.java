package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidLoginHint;
import net.openid.conformance.condition.client.SetRequestObjectLoginHintToInvalidBrazilConsentId;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-authorization-request-with-invalid-login-hint-fails-for-brazil",
	displayName = "FAPI-CIBA-ID1: Brazil CIBA invalid login_hint should return invalid_login_hint",
	summary = "This test sends a Brazil CIBA backchannel authentication request with login_hint set to an invalid consent identifier. The server must return an invalid_login_hint error.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBAID1EnsureAuthorizationRequestWithInvalidLoginHintFailsForBrazil extends AbstractFAPICIBAID1EnsureSendingInvalidBackchannelAuthorizationRequest {

	@Override
	protected void createAuthorizationRequestObject() {
		super.createAuthorizationRequestObject();
		callAndStopOnFailure(SetRequestObjectLoginHintToInvalidBrazilConsentId.class, "BrazilCIBA-6.2.3");
	}

	@Override
	protected void checkErrorFromBackchannelAuthorizationRequestResponse() {
		callAndContinueOnFailure(CheckErrorFromBackchannelAuthenticationEndpointErrorInvalidLoginHint.class,
			Condition.ConditionResult.FAILURE, "BrazilCIBA-6.2.3");
	}
}
