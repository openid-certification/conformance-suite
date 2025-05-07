package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.client.AddStateToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-final-state-only-outside-request-object-not-used",
	displayName = "FAPI2-Security-Profile-Final: check that a state value passed only outside request object is not used",
	summary = "This test uses a request object that does not contain state, but state is passed in the url parameters to the authorization endpoint (hence state should be ignored, as FAPI says only parameters inside the request object should be used). The expected result is a successful authentication that returns neither state nor s_hash. It is also permissible to show (a screenshot of which should be uploaded) or return an error message: invalid_request, invalid_request_object or access_denied.",
	profile = "FAPI2-Security-Profile-Final",
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
public class FAPI2SPFinalStateOnlyOutsideRequestObjectNotUsed extends AbstractFAPI2SPFinalEnsureRequestObjectWithoutState {

	@Override
	protected void performPARRedirectWithRequestUri() {
		// Note: BuildRequestObjectByReferenceRedirectToAuthorizationEndpoint and
		// BuildRequestObjectByValueRedirectToAuthorizationEndpoint include as URL
		// parameters values in "authorization_endpoint_request" which differ or are
		// missing from the request object. Here a state is added as a parameter.
		callAndStopOnFailure(AddStateToAuthorizationEndpointRequest.class);

		super.performPARRedirectWithRequestUri();
	}


}
