package net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule.v1;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.registrationData.v1.PersonalIdentificationResponseValidator;
import net.openid.conformance.openbanking_brasil.registrationData.v1.PersonalQualificationResponseValidator;
import net.openid.conformance.openbanking_brasil.registrationData.v1.PersonalRelationsResponseValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetPersonalFinancialRelationships;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareAllCustomerPersonalRelatedConsentsForHappyPathTest;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetPersonalIdentifications;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetPersonalQualifications;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "customer-personal-data-api-test",
	displayName = "Validate structure of all personal customer data API resources",
	summary = "Validates the structure of all personal customer data API resources\n" +
		"\u2022 Creates a Consent will the customer personal permissions (\"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\",\"CUSTOMERS_PERSONAL_ADITTIONALINFO_READ\",\"RESOURCES_READ\")\n" +
		"\u2022 Expects a success 201 - Check all of the fields sent on the consent API is spec compliant \n" +
		"\u2022 Calls GET Personal Qualifications resources\n" +
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
public class CustomerPersonalDataApiTestModule extends AbstractOBBrasilFunctionalTestModule {

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
		runInBlock("Validating personal qualifications response", () -> {
			callAndStopOnFailure(PrepareToGetPersonalQualifications.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(PersonalQualificationResponseValidator.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating personal financial relationship response", () -> {
			callAndContinueOnFailure(PrepareToGetPersonalFinancialRelationships.class); //ALEX
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(PersonalRelationsResponseValidator.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating personal identifications response", () -> {
			callAndStopOnFailure(PrepareToGetPersonalIdentifications.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(PersonalIdentificationResponseValidator.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		});


	}
}
