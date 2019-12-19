package net.openid.conformance.openid;

import net.openid.conformance.condition.client.AddPhoneScopeToAuthorizationEndpointRequest;
import net.openid.conformance.testmodule.PublishTestModule;

// Corresponds to OP-scope-phone
@PublishTestModule(
	testName = "oidcc-scope-phone",
	displayName = "OIDCC: check phone scope",
	summary = "This test requests authorization with phone scope.",
	profile = "OIDCC",
	configurationFields = {
			"server.discoveryUrl",
			"client.scope",
			"client2.scope",
			"resource.resourceUrl"
	}
)
public class OIDCCScopePhone extends AbstractOIDCCServerTest {

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.then(condition(AddPhoneScopeToAuthorizationEndpointRequest.class)));
	}

}
