package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
public class OIDCCScopePhone extends AbstractOIDCCScopesServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		JsonObject expectedScopes = new JsonParser().parse("{\"expected_scopes\": [\"phone\"]}").getAsJsonObject();
		env.putObject("expected_scopes", expectedScopes);

		super.skipTestIfScopesNotSupported();
	}

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.then(condition(AddPhoneScopeToAuthorizationEndpointRequest.class)));
	}

}
