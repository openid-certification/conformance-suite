package net.openid.conformance.openid;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
			"client.scope",
			"client2.scope",
			"resource.resourceUrl"
	}
)
public class OIDCCScopeAll extends AbstractOIDCCScopesServerTest {

	@Override
	protected void skipTestIfScopesNotSupported() {
		JsonObject expectedScopes = new JsonParser().parse("{\"expected_scopes\": [\"address\", \"email\", \"phone\", \"profile\"]}").getAsJsonObject();
		env.putObject("expected_scopes", expectedScopes);

		super.skipTestIfScopesNotSupported();
	}

	@Override
	protected void createAuthorizationRequest() {
		call(new CreateAuthorizationRequestSteps()
				.then(condition(AddAddressScopeToAuthorizationEndpointRequest.class),
						condition(AddEmailScopeToAuthorizationEndpointRequest.class),
						condition(AddPhoneScopeToAuthorizationEndpointRequest.class),
						condition(AddProfileScopeToAuthorizationEndpointRequest.class)));
	}

}
