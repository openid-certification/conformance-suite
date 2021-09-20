package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.plans.PrepareAllCustomerRelatedConsentsForResource404HappyPathTest;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "resources-api-test-404-customer-data",
	displayName = "Validate correct response when only request customer data permissions",
	summary = "Validates correct response when only request customer data permissions",
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
public class ResourcesApiTestModuleCorrect404 extends AbstractOBBrasilFunctionalTestModuleOptionalErrors {
	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndStopOnFailure(IgnoreResponseError.class);
		callAndStopOnFailure(PrepareAllCustomerRelatedConsentsForResource404HappyPathTest.class);
	}

	@Override
	protected void validateResponse() {

		String logMessage = String.format("Validate correct 404 api response");
		runInBlock(logMessage, () -> {
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.WARNING);
			callAndStopOnFailure(EnsureResponseCodeWas404.class);
		});
	}
}
