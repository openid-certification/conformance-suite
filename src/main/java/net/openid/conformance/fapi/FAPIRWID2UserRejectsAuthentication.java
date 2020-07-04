package net.openid.conformance.fapi;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddPromptLoginToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckMatchingCallbackParameters;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.ExpectAccessDeniedErrorFromAuthorizationEndpointDueToUserRejectingRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.Command;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPIProfile;
import net.openid.conformance.variant.FAPIRWOPProfile;

@PublishTestModule(
	testName = "fapi-rw-id2-user-rejects-authentication",
	displayName = "FAPI-RW-ID2: user rejects authentication",
	summary = "This test requires the user to reject the authentication, for example by pressing the 'cancel' button on the login screen. It verifies the error is correctly notified back to the relying party.",
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
public class FAPIRWID2UserRejectsAuthentication extends AbstractFAPIRWID2MultipleClient {

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		// Add length state with 128
		Command cmd = new Command();
		cmd.putInteger("requested_state_length", 128);

		ConditionSequence conditionSequence = super.makeCreateAuthorizationRequestSteps()
			.insertBefore(CreateRandomStateValue.class, cmd);

		if (getVariant(FAPIRWOPProfile.class) != FAPIRWOPProfile.OPENBANKING_UK) {
			conditionSequence.insertAfter(AddNonceToAuthorizationEndpointRequest.class, condition(AddPromptLoginToAuthorizationEndpointRequest.class));
		}

		return conditionSequence;
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(ExpectAccessDeniedErrorFromAuthorizationEndpointDueToUserRejectingRequest.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

		if (!isSecondClient()) {
			performAuthorizationFlowWithSecondClient();
		} else {

			// Check if server return correct params as we requested in redirect_uri query part
			callAndStopOnFailure(CheckMatchingCallbackParameters.class);
			fireTestFinished();
		}
	}
}
