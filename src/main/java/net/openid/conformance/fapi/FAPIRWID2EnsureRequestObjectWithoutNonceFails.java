package net.openid.conformance.fapi;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddAudToRequestObject;
import net.openid.conformance.condition.client.AddExpToRequestObject;
import net.openid.conformance.condition.client.AddIssToRequestObject;
import net.openid.conformance.condition.client.AddNonceToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import net.openid.conformance.condition.client.CheckStateInAuthorizationResponse;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.CreateAuthorizationEndpointRequestFromClientInformation;
import net.openid.conformance.condition.client.CreateRandomNonceValue;
import net.openid.conformance.condition.client.CreateRandomStateValue;
import net.openid.conformance.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import net.openid.conformance.condition.client.EnsureInvalidRequestOrInvalidRequestObjectError;
import net.openid.conformance.condition.client.ExpectRequestObjectMissingNonceErrorPage;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseModeToJWT;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCode;
import net.openid.conformance.condition.client.SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken;
import net.openid.conformance.condition.client.SignRequestObject;
import net.openid.conformance.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-request-object-without-nonce-fails",
	displayName = "FAPI-RW-ID2: ensure request object without nonce fails",
	summary = "This test should end with the authorisation server showing an error message that the request or request object is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
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
		"resource.resourceUrl",
		"resource.institution_id"
	}
)
public class FAPIRWID2EnsureRequestObjectWithoutNonceFails extends AbstractFAPIRWID2ExpectingAuthorizationFailure {

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRequestObjectMissingNonceErrorPage.class, "FAPI-RW-5.2.3-8");

		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected void createAuthorizationRequest() {
		callAndStopOnFailure(CreateAuthorizationEndpointRequestFromClientInformation.class);

		performProfileAuthorizationEndpointSetup();

		env.putInteger("requested_state_length", null);

		callAndStopOnFailure(CreateRandomStateValue.class);
		exposeEnvString("state");
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		if (jarm) {
			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCode.class);
			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseModeToJWT.class);
		} else {
			callAndStopOnFailure(SetAuthorizationEndpointRequestResponseTypeToCodeIdtoken.class);
		}
	}

	@Override
	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		callAndStopOnFailure(AddExpToRequestObject.class);

		callAndStopOnFailure(AddAudToRequestObject.class);

		callAndStopOnFailure(AddIssToRequestObject.class);

		callAndStopOnFailure(SignRequestObject.class);

		callAndStopOnFailure(CreateRandomNonceValue.class);
		exposeEnvString("nonce");
		callAndStopOnFailure(AddNonceToAuthorizationEndpointRequest.class);

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		// We now have callback_query_params and callback_params (containing the hash) available, as well as authorization_endpoint_response (which test conditions should use if they're looking for the response)

		/* If we get an error back from the authorisation server:
		 * - It must be a 'invalid_request_object' or 'invalid_request' error
		 * - It must have the correct state we supplied
		 */

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(EnsureInvalidRequestOrInvalidRequestObjectError.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6", "OIDCC-3.3.2.6");
		fireTestFinished();
	}
}
