package net.openid.conformance.openbanking_brasil.testmodules.v2.resources;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.resourcesAPI.v2.ResourcesResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.CustomerDataResources404;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.TestTimedOut;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;


//import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "resources-api-test-v2",
	displayName = "Validate structure of all resources API V2 resources",
	summary = "Validates the structure of all resources API V2 resources\n" +
		"\u2022 Creates a Consent will all of the existing permissions \n" +
		"\u2022 Checks all of the fields sent on the consent API are specification compliant\n" +
		"\u2022 Calls the GET resources API V2\n" +
		"\u2022 Expects a 200",
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
		"consent.productType"
	}
)
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class ResourcesApiTestModuleV2 extends AbstractOBBrasilFunctionalTestModuleOptionalErrors {

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildResourcesConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(IgnoreResponseError.class);
		callAndStopOnFailure(PrepareAllResourceRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(AddResourcesScope.class);
	}

	@Override
	protected void validateResponse() {

		ResourceApiV2PollingSteps pollingSteps = new ResourceApiV2PollingSteps(env, getId(),
			eventLog,testInfo, getTestExecutionManager());
		call(pollingSteps);

		String responseError = env.getString("resource_endpoint_error_code");
		if (Strings.isNullOrEmpty(responseError)) {
			String logMessage = "Validate resources V2 api request";
			runInBlock(logMessage, () -> {
				callAndStopOnFailure(ResourcesResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
				callAndStopOnFailure(EnsureResponseHasLinks.class);
				callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
				call(sequence(ValidateSelfEndpoint.class));
			});
		} else {
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas404.class);
			callAndStopOnFailure(CustomerDataResources404.class);
			callAndContinueOnFailure(ChuckWarning.class, Condition.ConditionResult.WARNING);
		}


	}
}
