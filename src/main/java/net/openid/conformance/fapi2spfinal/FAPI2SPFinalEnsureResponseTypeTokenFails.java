package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.CallPAREndpoint;
import net.openid.conformance.condition.client.CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.DetectWhetherErrorResponseIsInQueryOrFragment;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.condition.client.EnsurePARUnsupportedResponseTypeOrInvalidRequestOrUnauthorizedClientError;
import net.openid.conformance.condition.client.EnsureUnsupportedResponseTypeOrInvalidRequestError;
import net.openid.conformance.condition.client.RejectAuthCodeInUrlFragment;
import net.openid.conformance.condition.client.RejectAuthCodeInUrlQuery;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToToken;
import net.openid.conformance.condition.common.ExpectResponseTypeErrorPage;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-response-type-token-fails",
	displayName = "FAPI2-Security-Profile-Final: ensure response_type token fails",
	summary = "This test uses response_type=token in the authorization request, which is not permitted in FAPI2 Security Profile as it would return an access token via the browser where it may be leaked - only the authorization code flow ('response_type=code') is permitted. The authorization server should show an error message that the response type is unsupported or the request is invalid (a screenshot of which should be uploaded) or the user should be redirected back to the conformance suite with a correct error response, or an error could be returned from the PAR endpoint.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"resource.resourceUrl"
	}
)

@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "fapi_client_credentials_grant" })

public class FAPI2SPFinalEnsureResponseTypeTokenFails extends AbstractFAPI2SPFinalPARExpectingAuthorizationEndpointPlaceholderOrCallback {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectResponseTypeErrorPage.class, "FAPI2-SP-FINAL-5.3.2.2-1");

		env.putString("error_callback_placeholder", env.getString("response_type_error"));
	}

	@Override
	protected ConditionSequence makeCreateAuthorizationRequestSteps() {
		return super.makeCreateAuthorizationRequestSteps()
			.replace(SetAuthorizationEndpointRequestResponseTypeToCode.class,
				condition(SetAuthorizationEndpointRequestResponseTypeToToken.class).requirements("FAPI2-SP-FINAL-5.3.2.2-1"));
	}

	@Override
	protected void processParErrorResponse() {
		call(exec().mapKey("endpoint_response", CallPAREndpoint.RESPONSE_KEY));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, ConditionResult.FAILURE, "PAR-2.3");
		// we only raise a warning here as per https://bitbucket.org/openid/fapi/issues/618/certification-conformance-strictness-of
		callAndContinueOnFailure(EnsurePARUnsupportedResponseTypeOrInvalidRequestOrUnauthorizedClientError.class, Condition.ConditionResult.WARNING, "PAR-2.3");
		call(exec().unmapKey("endpoint_response"));
	}

	@Override
	protected void processCallback() {

		eventLog.startBlock(currentClientString() + "Verify authorization endpoint error response");

		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.3.2.2-1");
		callAndContinueOnFailure(RejectAuthCodeInUrlFragment.class, ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.3.2.2-1");

		// It doesn't really matter if the error in the fragment or the query, the specs aren't entirely clear on the matter
		callAndStopOnFailure(DetectWhetherErrorResponseIsInQueryOrFragment.class);

		/* The error from the authorization server:
		 * - must be a 'unsupported_response_type' or "invalid_request" error
		 * - must have the correct state we supplied
		 */
		callAndContinueOnFailure(EnsureUnsupportedResponseTypeOrInvalidRequestError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.6");

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckForUnexpectedParametersInErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");

		eventLog.endBlock();
		fireTestFinished();
	}
}
