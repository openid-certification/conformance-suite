package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.resourcesAPI.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareAllResourceRelatedConsentsForHappyPathTest;
import net.openid.conformance.testmodule.PublishTestModule;


//import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "resources-api-test",
	displayName = "Validate structure of all resources API resources",
	summary = "Validates the structure of all resources API resources",
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
public class ResourcesApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllResourceRelatedConsentsForHappyPathTest.class);
	}

	@Override
	protected void validateResponse() {

		String logMessage = String.format("Validate resources api request");
		runInBlock(logMessage, () -> {
			callAndStopOnFailure(ResourcesResponseValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
