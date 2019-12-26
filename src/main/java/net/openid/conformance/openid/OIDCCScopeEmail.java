package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
			"client.scope",
			"client2.scope",
			"resource.resourceUrl"
	}
)
public class OIDCCScopeEmail extends AbstractOIDCCScopesServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		JsonObject expectedScopes = new JsonParser().parse("{\"expected_scopes\": [\"email\"]}").getAsJsonObject();
		env.putObject("expected_scopes", expectedScopes);

		super.skipTestIfScopesNotSupported();
	}

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.then(condition(AddEmailScopeToAuthorizationEndpointRequest.class)));
	}

}
