package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.resourcesAPI.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareAllResourceRelatedConsentsForHappyPathTest;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareResourceRelatedConsentsForAccountsTest;
import net.openid.conformance.testmodule.PublishTestModule;


//import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "resources-api-test-accounts",
	displayName = "Validate structure of resources API - with ACCOUNTS_READ permissions",
	summary = "Validate structure of resources API - with ACCOUNTS_READ permissions",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.resourceUrl"
	}
)
public class ResourcesApiTestModuleAccount extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareResourceRelatedConsentsForAccountsTest.class);
	}

	@Override
	protected void validateResponse() {
		String logMessage = String.format("Validate resources api request for ACCOUNTS only");
		runInBlock(logMessage, () -> {
			callAndStopOnFailure(AccountResourcesResponseValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
