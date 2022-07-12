package net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.registrationData.v2.PersonalIdentificationResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.registrationData.v2.PersonalQualificationResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.registrationData.v2.PersonalRelationsResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddDummyPersonalProductTypeToConfig;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildPersonalCustomersConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas200;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateResponseMetaData;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "customer-personal-data-api-test-v2",
	displayName = "Validate structure of all personal customer data API resources",
	summary = "Validates the structure of all personal customer data API resources\n" +
		"\u2022 Creates a Consent will the customer personal permissions (\"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\",\"CUSTOMERS_PERSONAL_ADITTIONALINFO_READ\",\"RESOURCES_READ\")\n" +
		"\u2022 Expects a success 201 - Check all of the fields sent on the consent API is spec compliant \n" +
		"\u2022 Calls GET Personal Qualifications resources V2\n" +
		"\u2022 Expects a success 200",
	profile = OBBProfile.OBB_PROFILE,
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
public class CustomerPersonalDataApiTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {

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
		runInBlock("Validating personal qualifications response v2", () -> {
			callAndStopOnFailure(PrepareToGetPersonalQualifications.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(PersonalQualificationResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating personal financial relationship response V2", () -> {
			callAndContinueOnFailure(PrepareToGetPersonalFinancialRelationships.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(PersonalRelationsResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating personal identifications response V2", () -> {
			callAndStopOnFailure(PrepareToGetPersonalIdentifications.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(PersonalIdentificationResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		});


	}
}
