package net.openid.conformance.fapirwid2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddAccountRequestIdToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureAccessDeniedErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.ExpectAccessDeniedErrorPage;
import net.openid.conformance.condition.client.OpenBankingUkAddScaAcrClaimToAuthorizationEndpointRequest;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIRWOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "fapi-rw-id2-test-essential-acr-sca-claim",
	displayName = "FAPI-RW-ID2: check behaviour of openbankinguk server when an essential acr claim for SCA is made",
	summary = "This test requests an acr claim for SCA with essential=true, The server can choose to reject this by showing an access_denied error (a screenshot of which should be uploaded), or by the user being redirected back to the conformance suite with a correct error response. Alternatively the server can accept the request, in which case it must perform SCA and must return an acr in the id_token as SCA.",
	profile = "FAPI-RW-ID2",
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
// only applicable to UK OpenBanking as the SCA acr value we use is specific to that ecosystem
@VariantNotApplicable(parameter = FAPIRWOPProfile.class, values = { "plain_fapi", "consumerdataright_au" })
public class FAPIRWID2TestEssentialAcrScaClaim extends AbstractFAPIRWID2ExpectingAuthorizationEndpointPlaceholderOrCallback {

	@VariantSetup(parameter = FAPIRWOPProfile.class, value = "openbanking_uk")
	@Override
	public void setupOpenBankingUk() {
		super.setupOpenBankingUk();
		profileAuthorizationEndpointSetupSteps = OpenBankingUkAuthorizationEndpointOverridingSetup.class;
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectAccessDeniedErrorPage.class);

		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		JsonObject callbackParams = env.getObject("authorization_endpoint_response");
		if (callbackParams.has("error")) {
			callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
			callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
			callAndContinueOnFailure(EnsureAccessDeniedErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE);

			fireTestFinished();
		} else {
			super.onAuthorizationCallbackResponse();
		}
	}

	public static class OpenBankingUkAuthorizationEndpointOverridingSetup extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			callAndStopOnFailure(AddAccountRequestIdToAuthorizationEndpointRequest.class);

			callAndStopOnFailure(OpenBankingUkAddScaAcrClaimToAuthorizationEndpointRequest.class);
		}
	}
}
