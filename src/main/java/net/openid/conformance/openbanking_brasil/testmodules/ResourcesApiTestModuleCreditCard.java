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
	testName = "resources-api-test-credit-card",
	displayName = "Validate structure of resources API - with CREDIT_CARDS permissions",
	summary = "Validate structure of resources API - with CREDIT_CARDS permissions",
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
public class ResourcesApiTestModuleCreditCard extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareResourceRelatedConsentsForCreditCardTest.class);
	}

	@Override
	protected void validateResponse() {
		String logMessage = String.format("Validate resources api request for CREDIT_CARD_ACCOUNT only");
		runInBlock(logMessage, () -> {
			callAndStopOnFailure(CreditCardResourcesResponseValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
