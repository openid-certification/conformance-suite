package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddProfileScopeToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-profile
@PublishTestModule(
	testName = "oidcc-scope-profile",
	displayName = "OIDCC: check profile scope",
	summary = "This test requests authorization with profile scope.",
	profile = "OIDCC",
	configurationFields = {
			"server.discoveryUrl",
			"client.scope",
			"client2.scope",
			"resource.resourceUrl"
	}
)
public class OIDCCScopeProfile extends AbstractOIDCCServerTest {

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.then(condition(AddProfileScopeToAuthorizationEndpointRequest.class)));
	}

}
