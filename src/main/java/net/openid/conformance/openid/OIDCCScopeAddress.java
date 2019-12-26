package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
			"client.scope",
			"client2.scope",
			"resource.resourceUrl"
	}
)
public class OIDCCScopeAddress extends AbstractOIDCCScopesServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		JsonObject expectedScopes = new JsonParser().parse("{\"expected_scopes\": [\"address\"]}").getAsJsonObject();
		env.putObject("expected_scopes", expectedScopes);

		super.skipTestIfScopesNotSupported();
	}

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.then(condition(AddAddressScopeToAuthorizationEndpointRequest.class)));
	}

}
