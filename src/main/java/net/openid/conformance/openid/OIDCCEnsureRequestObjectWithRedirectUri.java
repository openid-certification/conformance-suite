package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddInvalidRedirectUriToAuthorizationRequest;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.testmodule.PublishTestModule;

// This does not correspond to a particular OIDC python test
@PublishTestModule(
	testName = "oidcc-ensure-request-object-with-redirect-uri",
	displayName = "OIDCC: ensure request object redirect_uri takes precedence",
	summary = "This test includes two redirect URIs, one in the request object (passed by value) and one as a normal request parameter. The server must either use the redirect_uri in the request object (as per OIDCC-6.1) and process the authentication correctly, or return a request_not_supported error as per OIDCC-3.1.2.6. This is an extra test that wasn't present in the python suite, and ensures implementations are processing request objects correctly.",
	profile = "OIDCC"
)
public class OIDCCEnsureRequestObjectWithRedirectUri extends AbstractOIDCCRequestObjectServerTest {

	@Override
	protected void createAuthorizationRedirect() {
		call(new CreateAuthorizationRedirectSteps()
				.insertAfter(ConvertAuthorizationEndpointRequestToRequestObject.class,
						condition(AddInvalidRedirectUriToAuthorizationRequest.class).requirement("OIDCC-6.1")));
	}

}
