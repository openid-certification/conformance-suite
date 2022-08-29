package net.openid.conformance.openinsurance.testmodule.resources.v1;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.resourcesAPI.v1.ResourcesResponseValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.CustomerDataResources404;
import net.openid.conformance.openinsurance.testmodule.support.OpinConsentPermissionsBuilder;
import net.openid.conformance.openinsurance.testmodule.support.PermissionsGroup;
import net.openid.conformance.openinsurance.validator.resources.v1.OpinResourcesListValidatorV1;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;


@PublishTestModule(
	testName = "opin-resources-api-test",
	displayName = "Validate structure of all resources API resources",
	summary = "Validates the structure of all resources API resources\n" +
		"\u2022 Creates a Consent will all of the existing permissions \n" +
		"\u2022 Checks all of the fields sent on the consent API are specification compliant\n" +
		"\u2022 Calls the GET resources API\n" +
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
public class OpinResourcesApiTestModule extends AbstractOBBrasilFunctionalTestModuleOptionalErrors {

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildResourcesConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		OpinConsentPermissionsBuilder permissionsBuilder = new OpinConsentPermissionsBuilder(env,getId(),eventLog,testInfo,executionManager);
		permissionsBuilder.addPermissionsGroup(PermissionsGroup.ALL);

		String productType = env.getString("config", "consent.productType");
		if (!Strings.isNullOrEmpty(productType) && productType.equals("business")) {
			permissionsBuilder.removePermissionsGroups(PermissionsGroup.CUSTOMERS_PERSONAL);
		}
		if (!Strings.isNullOrEmpty(productType) && productType.equals("personal")) {
			permissionsBuilder.removePermissionsGroups(PermissionsGroup.CUSTOMERS_BUSINESS);
		}

		permissionsBuilder.build();

		callAndStopOnFailure(IgnoreResponseError.class);
		callAndStopOnFailure(AddResourcesScope.class);
	}

	@Override
	protected void validateResponse() {

		String responseError = env.getString("resource_endpoint_error_code");
		if (Strings.isNullOrEmpty(responseError)) {
			String logMessage = "Validate resources api request";
			runInBlock(logMessage, () -> {
				callAndStopOnFailure(OpinResourcesListValidatorV1.class, Condition.ConditionResult.FAILURE);
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
