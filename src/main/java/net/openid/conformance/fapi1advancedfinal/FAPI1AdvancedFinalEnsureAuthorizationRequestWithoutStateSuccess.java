package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CheckIfAuthorizationEndpointError;
import net.openid.conformance.condition.client.CheckMatchingCallbackParameters;
import net.openid.conformance.condition.client.EnsureMinimumAuthorizationCodeEntropy;
import net.openid.conformance.condition.client.EnsureMinimumAuthorizationCodeLength;
import net.openid.conformance.condition.client.ExtractAuthorizationCodeFromAuthorizationResponse;
import net.openid.conformance.condition.client.VerifyNoStateInAuthorizationResponse;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-ensure-authorization-request-without-state-success",
	displayName = "FAPI1-Advanced-Final: ensure authorization endpoint request without state success",
	summary = "This test makes an authentication request that does not include 'state'. State is an optional parameter, so the authorization server must successfully authenticate and must not return state nor s_hash.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPI1AdvancedFinalEnsureAuthorizationRequestWithoutStateSuccess extends AbstractFAPI1AdvancedFinalEnsureRequestObjectWithoutState {

	@Override
	protected void performRedirect() {
		super.performNormalRedirect();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		callAndStopOnFailure(CheckMatchingCallbackParameters.class);

		callAndStopOnFailure(CheckIfAuthorizationEndpointError.class);

		callAndContinueOnFailure(VerifyNoStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(ExtractAuthorizationCodeFromAuthorizationResponse.class);

		callAndContinueOnFailure(EnsureMinimumAuthorizationCodeLength.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10", "RFC6819-5.1.4.2-2");

		callAndContinueOnFailure(EnsureMinimumAuthorizationCodeEntropy.class, Condition.ConditionResult.FAILURE, "RFC6749-10.10", "RFC6819-5.1.4.2-2");

		handleSuccessfulAuthorizationEndpointResponse();
	}
}
