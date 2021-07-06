package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.registrationData.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "customer-business-data-api-test",
	displayName = "Validate structure of all business customer data API resources",
	summary = "Validates the structure of all business customer data API resources",
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
public class CustomerBusinessDataApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndStopOnFailure(PrepareAllCustomerBusinessRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(PrepareToGetBusinessFinancialRelations.class);
	}

	@Override
	protected void validateResponse() {
		runInBlock("Validating corporate relationship response", () ->{
			callAndContinueOnFailure(CallProtectedResourceWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CorporateRelationshipResponseValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating business identifications response", () -> {
			callAndStopOnFailure(PrepareToGetBusinessIdentifications.class);
			callAndContinueOnFailure(CallProtectedResourceWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(LegalEntityIdentificationValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating business qualifications response", () -> {
			callAndStopOnFailure(PrepareToGetBusinessQualifications.class);
			callAndContinueOnFailure(CallProtectedResourceWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(LegalEntityQualificationResponseValidator.class, Condition.ConditionResult.FAILURE);
		});

	}
}
