package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddInvalidRedirectUriToAuthorizationRequest;
import net.openid.conformance.condition.client.ConvertAuthorizationEndpointRequestToRequestObject;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "oidcc-ensure-request-object-with-redirect-uri",
	displayName = "OIDCC: ensure request object redirect_uri takes precedence",
	summary = "This test includes two redirect URIs, one in the request object and one as a request parameter. The server should use the value in the request object.",
	profile = "OIDCC",
	configurationFields = {
			"server.discoveryUrl",
			"client.client_id",
			"client.scope",
			"client2.client_id",
			"client2.scope",
			"resource.resourceUrl"
	}
)
public class OIDCCEnsureRequestObjectWithRedirectUri extends AbstractOIDCCRequestObjectServerTest {

	@Override
	protected void createAuthorizationRedirect() {
		call(new CreateAuthorizationRedirectSteps()
				.insertAfter(ConvertAuthorizationEndpointRequestToRequestObject.class,
						condition(AddInvalidRedirectUriToAuthorizationRequest.class)));
	}

}
