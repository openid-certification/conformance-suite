package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddExpToRequestObject;
import io.fintechlabs.testframework.condition.client.BuildRequestObjectRedirectToAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObject;
import io.fintechlabs.testframework.condition.client.CheckStateInAuthorizationResponse;
import io.fintechlabs.testframework.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import io.fintechlabs.testframework.condition.client.EnsureErrorFromAuthorizationEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExpectRequestObjectMissingRedirectUriErrorPage;
import io.fintechlabs.testframework.condition.client.RemoveRedirectUriFromRequestObject;
import io.fintechlabs.testframework.condition.client.SignRequestObject;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;
import io.fintechlabs.testframework.testmodule.PublishTestModule;
import io.fintechlabs.testframework.testmodule.Variant;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-request-object-without-redirect-uri-fails",
	displayName = "FAPI-RW-ID2: ensure request object without redirect_uri fails",
	summary = "This test should end with the authorisation server showing an error message that the request object is invalid (a screenshot of which should be uploaded) or with the user being redirected back to the conformance suite with a correct error response.",
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
public class FAPIRWID2EnsureRequestObjectWithoutRedirectUriFails extends AbstractFAPIRWID2ServerTestModule {

	@Variant(name = variant_mtls)
	public void setupMTLS() {
		super.setupMTLS();
	}

	@Variant(name = variant_privatekeyjwt)
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
	}

	@Variant(
		name = variant_openbankinguk_mtls,
		configurationFields = {
			"resource.resourceUrlAccountRequests",
			"resource.resourceUrlAccountsResource",
		}
	)
	public void setupOpenBankingUkMTLS() {
		super.setupOpenBankingUkMTLS();
	}

	@Variant(
		name = variant_openbankinguk_privatekeyjwt,
		configurationFields = {
			"resource.resourceUrlAccountRequests",
			"resource.resourceUrlAccountsResource",
		}
	)
	public void setupOpenBankingUkPrivateKeyJwt() {
		super.setupOpenBankingUkPrivateKeyJwt();
	}

	@Override
	protected void performAuthorizationFlow() {
		performPreAuthorizationSteps();

		createAuthorizationRequest();

		createAuthorizationRedirect();

		performRedirectAndWaitForErrorCallback();
	}

	@Override
	protected void createPlaceholder() {
		callAndStopOnFailure(ExpectRequestObjectMissingRedirectUriErrorPage.class, "FAPI-RW-5.2.3-8");

		env.putString("error_callback_placeholder", env.getString("request_object_unverifiable_error"));
	}

	@Override
	protected void createAuthorizationRedirect() {
		callAndStopOnFailure(ConvertAuthorizationEndpointRequestToRequestObject.class);

		callAndStopOnFailure(AddExpToRequestObject.class);

		callAndStopOnFailure(RemoveRedirectUriFromRequestObject.class);

		callAndStopOnFailure(SignRequestObject.class);

		callAndStopOnFailure(BuildRequestObjectRedirectToAuthorizationEndpoint.class);
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		// We now have callback_query_params and callback_params (containing the hash) available, as well as authorization_endpoint_response (which test conditions should use if they're looking for the response)

		/* If we get an error back from the authorisation server:
		 * - It must be a 'invalid_request_object' error
		 * - It must have the correct state we supplied
		 */

		callAndContinueOnFailure(CheckStateInAuthorizationResponse.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureErrorFromAuthorizationEndpointResponse.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(CheckErrorFromAuthorizationEndpointErrorInvalidRequestOrInvalidRequestObject.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		fireTestFinished();
	}
}
