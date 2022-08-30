package net.openid.conformance.openinsurance.testmodule.customers.v1;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddDummyBusinessProductTypeToConfig;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildBusinessCustomersConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas200;
import net.openid.conformance.openinsurance.testmodule.support.*;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersBusinessComplimentaryInformationListValidatorV1;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersBusinessIdentificationListValidatorV1;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersBusinessQualificationListValidatorV1;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "opin-customers-business-data-api-test-v1",
	displayName = "Validate structure of all business customer data API resources V1",
	summary = "Validates the structure of all business customer data API resources V1\n" +
		"\u2022 Creates a Consent with the customer business permissions (\"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\",\"CUSTOMERS_BUSINESS_ADITTIONALINFO_READ\",\"RESOURCES_READ\")\n" +
		"\u2022 Expects a success 201 - Check all of the fields sent on the consent API is spec compliant \n" +
		"\u2022 Calls GET Personal Identifications Endpoint\n" +
		"\u2022 Expects a success 200 - Validate all Fields\n" +
		"\u2022 Calls GET Personal Qualifications Endpoint\n" +
		"\u2022 Expects a success 200 - Validate all Fields\n" +
		"\u2022 Calls GET Personal Complimentary-Information Endpoint\n" +
		"\u2022 Expects a success 200 - Validate all Fields\n",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE2,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf"
	}
)
public class OpinCustomersBusinessDataApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildBusinessCustomersConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		OpinConsentPermissionsBuilder permissionsBuilder = new OpinConsentPermissionsBuilder(env,getId(),eventLog,testInfo,executionManager);
		permissionsBuilder.addPermissionsGroup(PermissionsGroup.CUSTOMERS_BUSINESS);
		permissionsBuilder.build();

		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndStopOnFailure(PrepareToGetBusinessQualifications.class);
		callAndStopOnFailure(AddDummyBusinessProductTypeToConfig.class);
	}

	@Override
	protected void validateResponse() {

		runInBlock("Validating business identifications response V1", () -> {
			callAndStopOnFailure(PrepareToGetBusinessIdentifications.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(OpinCustomersBusinessIdentificationListValidatorV1.class, Condition.ConditionResult.FAILURE);
		});
		runInBlock("Validating business qualifications response V1", () -> {
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(OpinCustomersBusinessQualificationListValidatorV1.class, Condition.ConditionResult.FAILURE);
		});
		runInBlock("Validating business complimentary-information response V1", () ->{
			callAndStopOnFailure(PrepareToGetBusinessComplimentaryInformation.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(OpinCustomersBusinessComplimentaryInformationListValidatorV1.class, Condition.ConditionResult.FAILURE);
		});


	}
}
