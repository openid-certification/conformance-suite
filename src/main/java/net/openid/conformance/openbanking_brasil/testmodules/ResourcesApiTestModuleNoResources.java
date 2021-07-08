package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.resourcesAPI.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;


//import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "resources-api-test-no-resources",
	displayName = "Validate structure of resources API - without any resource permissions",
	summary = "Validates the structure of all resources API - without any resource permissions",
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
public class ResourcesApiTestModuleNoResources extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllResourceRelatedConsentsForEmptyResourcesTest.class);
	}

	@Override
	protected void validateResponse() {
		String logMessage = String.format("Validate resources api request with no permissions requested");
		runInBlock(logMessage, () -> {
			callAndStopOnFailure(EmptyResourcesResponseValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
