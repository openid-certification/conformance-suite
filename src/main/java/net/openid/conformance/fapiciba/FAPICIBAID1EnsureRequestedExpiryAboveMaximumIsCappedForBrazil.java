package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.EnsureOpenBankingBrazilCibaExpiresInDoesNotExceedMaximum;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPICIBAProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi-ciba-id1-ensure-requested-expiry-above-maximum-is-capped-for-brazil",
	displayName = "FAPI-CIBA-ID1: Ensure requested_expiry is capped for Brazil",
	summary = "This test requests an authentication lifetime one second above the configured Open Finance Brasil product or service maximum. The authorization server must return an expires_in value that does not exceed that maximum.",
	profile = "FAPI-CIBA-ID1"
)
@VariantNotApplicable(parameter = FAPICIBAProfile.class,
	values = {"plain_fapi", "openbanking_uk", "connectid_au"})
public class FAPICIBAID1EnsureRequestedExpiryAboveMaximumIsCappedForBrazil
	extends AbstractFAPICIBAID1 {

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		callAndStopOnFailure(AddRequestedExpiryAboveConfiguredMaximumToAuthorizationEndpointRequest.class,
			"BrazilCIBA-6.2.6");
	}

	@Override
	protected void performValidateAuthorizationResponse() {
		super.performValidateAuthorizationResponse();
		// Keep the module's defining assertion explicit even though the Brazil profile validation
		// enforces the same upper bound, so this module cannot silently degrade to a happy path.
		callAndContinueOnFailure(
			EnsureOpenBankingBrazilCibaExpiresInDoesNotExceedMaximum.class,
			Condition.ConditionResult.FAILURE,
			"BrazilCIBA-6.2.6");
	}
}
