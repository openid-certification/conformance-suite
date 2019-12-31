package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
public class OIDCCScopeProfile extends AbstractOIDCCScopesServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		JsonObject expectedScopes = new JsonParser().parse("{\"expected_scopes\": [\"profile\"]}").getAsJsonObject();
		env.putObject("expected_scopes", expectedScopes);

		super.skipTestIfScopesNotSupported();
	}

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.then(condition(AddProfileScopeToAuthorizationEndpointRequest.class)));
	}

}
