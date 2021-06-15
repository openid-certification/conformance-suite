package net.openid.conformance.openbanking_brasil;

import com.google.gson.JsonObject;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "account-get-test",
	displayName = "Validate structure of a single account resource",
	summary = "Validates the structure of the accounts API",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.resourceUrl"
	}
)
public class AccountGetTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void validateResponse() {

	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		super.preCallProtectedResource("Fetch all accounts");
		System.out.println("HEWRE");
	}
}
