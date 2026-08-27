package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddCdrSharingDurationClaimNegativeToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CdrEnsureNegativeSharingDurationRejected;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestError;
import net.openid.conformance.condition.client.ExpectNegativeSharingDurationErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-cdr-negative-sharing-duration",
	displayName = "FAPI2-Security-Profile-Final: CDR test that a negative sharing_duration is rejected",
	summary = "This test requests authorisation with a negative sharing_duration. The CDR standards say the authorisation SHOULD fail, either at the PAR endpoint or at the authorization endpoint; as this is only a SHOULD, a Data Holder that allows the authorisation to complete receives a warning rather than a failure.",
	profile = "FAPI2-Security-Profile-Final"
)
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi", "openbanking_brazil", "connectid_au", "cbuae", "ksa", "fapi_client_credentials_grant", "vci", "vci_haip" })
public class FAPI2SPFinalCdrEnsureNegativeSharingDurationFails extends AbstractFAPI2SPFinalPARExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps(boolean usePkce) {
		// added after the CDR profile setup steps, so this overwrites the sharing_duration those add
		return super.makeCreateAuthorizationRequestSteps(usePkce)
			.then(condition(AddCdrSharingDurationClaimNegativeToAuthorizationEndpointRequest.class)
				.requirements("CDR-request-object"));
	}

	@Override
	protected void createPlaceholder() {
		callAndContinueOnFailure(ExpectNegativeSharingDurationErrorPage.class, Condition.ConditionResult.WARNING);
		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected void processParErrorResponse() {
		callAndContinueOnFailure(EnsurePARInvalidRequestError.class, Condition.ConditionResult.WARNING, "RFC9126-2.3");
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		JsonObject callbackParams = env.getObject("authorization_endpoint_response");

		if (!callbackParams.has("error")) {

			// the authorisation succeeded; CDR says it SHOULD fail, so warn and complete the flow normally
			callAndContinueOnFailure(CdrEnsureNegativeSharingDurationRejected.class, Condition.ConditionResult.WARNING, "CDR-request-object");

			super.onAuthorizationCallbackResponse();

		} else {

			callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);

			callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

			callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");

			fireTestFinished();
		}
	}
}
