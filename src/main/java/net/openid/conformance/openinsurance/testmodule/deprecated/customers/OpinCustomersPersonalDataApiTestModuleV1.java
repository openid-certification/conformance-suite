package net.openid.conformance.openinsurance.testmodule.deprecated.customers;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddDummyPersonalProductTypeToConfig;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildPersonalCustomersConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas200;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateResponseMetaData;
import net.openid.conformance.openinsurance.testmodule.support.AddScopesForCustomerApi;
import net.openid.conformance.openinsurance.testmodule.support.PrepareAllCustomerPersonalRelatedConsentsForHappyPathTest;
import net.openid.conformance.openinsurance.testmodule.support.PrepareToGetPersonalComplimentaryInformation;
import net.openid.conformance.openinsurance.testmodule.support.PrepareToGetPersonalIdentifications;
import net.openid.conformance.openinsurance.testmodule.support.PrepareToGetPersonalQualifications;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersPersonalComplimentaryInformationListValidatorV1;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersPersonalIdentificationListValidatorV1;
import net.openid.conformance.openinsurance.validator.customers.v1.OpinCustomersPersonalQualificationListValidatorV1;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "opin-customer-personal-data-api-test-v2",
	displayName = "Validate structure of all personal customer data API V1 resources",
	summary = "Validates the structure of all personal customer data API V1 resources\n" +
		"\u2022 Creates a Consent will the customer personal permissions (\"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\",\"CUSTOMERS_PERSONAL_ADITTIONALINFO_READ\",\"RESOURCES_READ\")\n" +
		"\u2022 Expects a success 201 - Check all of the fields sent on the consent API V1 is spec compliant \n" +
		"\u2022 Calls GET Personal Qualifications resources V2\n" +
		"\u2022 Expects a success 200",
	profile = OBBProfile.OBB_PROFILE_OPEN_INSURANCE_PHASE1,
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
public class OpinCustomersPersonalDataApiTestModuleV1 extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildPersonalCustomersConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndStopOnFailure(PrepareAllCustomerPersonalRelatedConsentsForHappyPathTest.class);
		callAndContinueOnFailure(PrepareToGetPersonalQualifications.class);
		callAndStopOnFailure(AddDummyPersonalProductTypeToConfig.class);
	}

	@Override
	protected void validateResponse() {
		runInBlock("Validating personal qualifications response v1", () -> {
			callAndStopOnFailure(PrepareToGetPersonalQualifications.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(OpinCustomersPersonalQualificationListValidatorV1.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating personal complimentary-information response V1", () -> {
			callAndContinueOnFailure(PrepareToGetPersonalComplimentaryInformation.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(OpinCustomersPersonalComplimentaryInformationListValidatorV1.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating personal identifications response V1", () -> {
			callAndStopOnFailure(PrepareToGetPersonalIdentifications.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(OpinCustomersPersonalIdentificationListValidatorV1.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		});


	}
}
