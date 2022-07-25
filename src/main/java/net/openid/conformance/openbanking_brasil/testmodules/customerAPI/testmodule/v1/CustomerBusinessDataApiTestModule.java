package net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule.v1;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.registrationData.v1.BusinessIdentificationValidator;
import net.openid.conformance.openbanking_brasil.registrationData.v1.BusinessQualificationResponseValidator;
import net.openid.conformance.openbanking_brasil.registrationData.v1.BusinessRelationsResponseValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareAllCustomerBusinessRelatedConsentsForHappyPathTest;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetBusinessFinancialRelations;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetBusinessIdentifications;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetBusinessQualifications;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "customer-business-data-api-test",
	displayName = "Validate structure of all business customer data API resources",
	summary = "Validates the structure of all business customer data API resources\n" +
		"\u2022 Creates a Consent with the customer business permissions (\"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\",\"CUSTOMERS_BUSINESS_ADITTIONALINFO_READ\",\"RESOURCES_READ\")\n" +
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
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"directory.client_id",
	"client.org_jwks"
})
public class CustomerBusinessDataApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildBusinessCustomersConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndStopOnFailure(PrepareAllCustomerBusinessRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(PrepareToGetBusinessQualifications.class);
		callAndStopOnFailure(AddDummyBusinessProductTypeToConfig.class);
	}

	@Override
	protected void validateResponse() {
		runInBlock("Validating business qualifications response", () -> {
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(BusinessQualificationResponseValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating business identifications response", () -> {
			callAndStopOnFailure(PrepareToGetBusinessIdentifications.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(BusinessIdentificationValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating corporate relationship response", () ->{
			callAndStopOnFailure(PrepareToGetBusinessFinancialRelations.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(BusinessRelationsResponseValidator.class, Condition.ConditionResult.FAILURE);
		});


	}
}
