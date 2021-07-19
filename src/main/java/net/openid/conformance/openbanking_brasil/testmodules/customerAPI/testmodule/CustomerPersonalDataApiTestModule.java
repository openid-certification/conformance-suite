package net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.registrationData.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.PrepareToGetPersonalFinancialRelationships;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "customer-personal-data-api-test",
	displayName = "Validate structure of all personal customer data API resources",
	summary = "Validates the structure of all personal customer data API resources",
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
			callAndContinueOnFailure(CallProtectedResourceWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(NaturalPersonRelationshipResponseValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating personal identifications response", () -> {
			callAndStopOnFailure(PrepareToGetPersonalIdentifications.class);
			callAndContinueOnFailure(CallProtectedResourceWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(NaturalPersonIdentificationResponseValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating personal qualifications response", () -> {
			callAndStopOnFailure(PrepareToGetPersonalQualifications.class);
			callAndContinueOnFailure(CallProtectedResourceWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(NaturalPersonalQualificationResponseValidator.class, Condition.ConditionResult.FAILURE);
		});

	}
}
