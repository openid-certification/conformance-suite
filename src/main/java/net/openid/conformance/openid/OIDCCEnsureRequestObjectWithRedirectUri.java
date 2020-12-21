package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddInvalidRedirectUriToAuthorizationRequest;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.condition.client.EnsureOPDoesNotUseDefaultRedirectUriInCaseOfInvalidRedirectUri;
import net.openid.conformance.condition.common.ExpectRedirectUriErrorPage;
import net.openid.conformance.testmodule.PublishTestModule;

// This does not correspond to a particular OIDC python test
@PublishTestModule(
	testName = "oidcc-ensure-request-object-with-redirect-uri",
	displayName = "OIDCC: ensure request object redirect_uri takes precedence",
	summary = "This test includes two redirect URIs, one in the request object (passed by value) " +
		"and one as a normal request parameter. The server must either use the redirect_uri in the request object " +
		"(as per OIDCC-6.1) and process the authentication correctly, " +
		"or show an invalid redirect_uri error - upload a screenshot of the error page. " +
		"This is an extra test that wasn't present in the python suite, and ensures implementations are " +
		"processing request objects correctly. The test will be skipped if the server discovery document indicates the server only supports signed request objects (i.e. the server does not support unsigned request objects, indicated by 'request_object_signing_alg_values_supported' not containing 'none').",
	profile = "OIDCC"
)
public class OIDCCEnsureRequestObjectWithRedirectUri extends AbstractOIDCCRequestObjectServerTestExpectingRedirectOrPlaceholder {

	@Override
	protected void createAuthorizationRedirect() {
		call(new CreateAuthorizationRedirectSteps()
				.insertAfter(ConvertAuthorizationEndpointRequestToRequestObject.class,
						condition(AddInvalidRedirectUriToAuthorizationRequest.class).requirement("OIDCC-6.1")));
	}

	@Override
	protected void createPlaceholder()
	{
		callAndStopOnFailure(ExpectRedirectUriErrorPage.class, "RFC6749-3.1.2");

		env.putString("error_callback_placeholder", env.getString("redirect_uri_error"));
	}

	@Override
	protected void onAuthorizationCallbackResponse() {
		String error = env.getString("authorization_endpoint_response", "error");
		if (error != null && error.equals("request_not_supported")) {
			//this is unexpected as the redirect_uri outside the request object was invalid
			//but we received a redirect to the correct redirect_uri
			callAndStopOnFailure(EnsureOPDoesNotUseDefaultRedirectUriInCaseOfInvalidRedirectUri.class);
		}

		super.onAuthorizationCallbackResponse();
	}
}
