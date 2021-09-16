package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.resourcesAPI.*;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareAllResourceRelatedConsentsForHappyPathTest;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.CustomerDataResources404;
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
public class ResourcesApiTestModule extends AbstractOBBrasilFunctionalTestModuleOptionalErrors {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(IgnoreResponseError.class);
		callAndStopOnFailure(PrepareAllResourceRelatedConsentsForHappyPathTest.class);
	}

	@Override
	protected void validateResponse() {

		String responseError = env.getString("resource_endpoint_error_code");
		if (Strings.isNullOrEmpty(responseError)) {
			String logMessage = String.format("Validate resources api request");
			runInBlock(logMessage, () -> {
				callAndStopOnFailure(ResourcesResponseValidator.class, Condition.ConditionResult.FAILURE);
				callAndStopOnFailure(EnsureResponseHasLinks.class);
			});
		} else {
			callAndStopOnFailure(EnsureResponseCodeWas404.class);
			callAndStopOnFailure(CustomerDataResources404.class);
			callAndContinueOnFailure(ChuckWarning.class, Condition.ConditionResult.WARNING);
		}


	}
}
