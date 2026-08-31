package net.openid.conformance.fapi2spfinal;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddUnknownCdrArrangementIdClaimToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CdrEnsureUnrecognisedArrangementIdRejected;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsurePARInvalidRequestError;
import net.openid.conformance.condition.client.ExpectUnrecognisedArrangementIdErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-cdr-unrecognised-arrangement-id",
	displayName = "FAPI2-Security-Profile-Final: CDR test that an unrecognised cdr_arrangement_id is rejected",
	summary = "This test sends an authorisation request containing a cdr_arrangement_id that the Data Holder cannot recognise. The CDR standards require such requests to be rejected, either at the PAR endpoint or at the authorization endpoint.",
	profile = "FAPI2-Security-Profile-Final"
)
@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "plain_fapi", "openbanking_brazil", "connectid_au", "cbuae", "openbanking_chile", "ksa", "fapi_client_credentials_grant", "vci", "vci_haip" })
public class FAPI2SPFinalCdrEnsureUnrecognisedArrangementIdFails extends AbstractFAPI2SPFinalPARExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps(boolean usePkce) {
		return super.makeCreateAuthorizationRequestSteps(usePkce)
			.then(condition(AddUnknownCdrArrangementIdClaimToAuthorizationEndpointRequest.class)
				.requirements("CDR-request-object"));
	}

	@Override
	protected void createPlaceholder() {
		callAndContinueOnFailure(ExpectUnrecognisedArrangementIdErrorPage.class, Condition.ConditionResult.WARNING);
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

			callAndContinueOnFailure(CdrEnsureUnrecognisedArrangementIdRejected.class, Condition.ConditionResult.FAILURE, "CDR-request-object");

			fireTestFinished();

		} else {

			callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);

			callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

			callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");

			fireTestFinished();
		}
	}
}
