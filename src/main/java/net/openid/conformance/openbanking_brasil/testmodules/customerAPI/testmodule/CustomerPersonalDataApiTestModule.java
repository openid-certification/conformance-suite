package net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.registrationData.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetPersonalFinancialRelationships;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareAllCustomerPersonalRelatedConsentsForHappyPathTest;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetPersonalIdentifications;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetPersonalQualifications;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas200;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateResponseMetaData;
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
		"resource.brazilCpf",
		"resource.resourceUrl"
	}
)
public class CustomerPersonalDataApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndStopOnFailure(PrepareAllCustomerPersonalRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(PrepareToGetPersonalFinancialRelationships.class);
	}

	@Override
	protected void validateResponse() {
		runInBlock("Validating personal financial relationship response", () -> {
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

		runInBlock("Validating personal qualifications response", () -> {
			callAndStopOnFailure(PrepareToGetPersonalQualifications.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(PersonalQualificationResponseValidator.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		});
	}
}
