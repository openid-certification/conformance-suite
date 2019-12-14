package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddAddressScopeToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-address
@PublishTestModule(
	testName = "oidcc-scope-address",
	displayName = "OIDCC: check address scope",
	summary = "This test requests authorization with address scope.",
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
public class OIDCCScopeAddress extends AbstractOIDCCServerTest {

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.then(condition(AddAddressScopeToAuthorizationEndpointRequest.class)));
	}

}
