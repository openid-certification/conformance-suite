package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddEmailScopeToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-email
@PublishTestModule(
	testName = "oidcc-scope-email",
	displayName = "OIDCC: check email scope",
	summary = "This test requests authorization with email scope.",
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
public class OIDCCScopeEmail extends AbstractOIDCCServerTest {

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.then(condition(AddEmailScopeToAuthorizationEndpointRequest.class)));
	}

}
