package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddRequestedExp13sToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckBackchannelExpiresInDoesNotMatchRequestedExpiry;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-requested-expiry-is-ignored-for-brazil",
	displayName = "FAPI-CIBA-ID1: Ensure requested_expiry is ignored for Brazil",
	summary = "This test makes a CIBA request with a short requested_expiry and checks that this value did not influence expires_in in the backchannel response. If expires_in matches requested_expiry, the test raises a warning.",
	profile = "FAPI-CIBA-ID1",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"client.hint_type",
		"client.hint_value",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class, values = {"plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBAID1EnsureRequestedExpiryIgnoredForBrazil extends AbstractFAPICIBAID1 {

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		callAndStopOnFailure(AddRequestedExp13sToAuthorizationEndpointRequest.class);
	}

	@Override
	protected void performValidateAuthorizationResponse() {
		super.performValidateAuthorizationResponse();
		callAndContinueOnFailure(CheckBackchannelExpiresInDoesNotMatchRequestedExpiry.class, Condition.ConditionResult.WARNING, "BrazilCIBA-6.2.6");
	}
}
