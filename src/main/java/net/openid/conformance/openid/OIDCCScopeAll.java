package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddAddressScopeToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddEmailScopeToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddPhoneScopeToAuthorizationEndpointRequest;
import net.openid.conformance.condition.client.AddProfileScopeToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-all
@PublishTestModule(
	testName = "oidcc-scope-all",
	displayName = "OIDCC: check all scopes",
	summary = "This test requests authorization with address, email, phone and profile scopes.",
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
public class OIDCCScopeAll extends AbstractOIDCCServerTest {

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.then(condition(AddAddressScopeToAuthorizationEndpointRequest.class),
						condition(AddEmailScopeToAuthorizationEndpointRequest.class),
						condition(AddPhoneScopeToAuthorizationEndpointRequest.class),
						condition(AddProfileScopeToAuthorizationEndpointRequest.class)));
	}

}
