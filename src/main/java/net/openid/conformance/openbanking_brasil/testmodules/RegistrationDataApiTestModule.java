package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResourceWithBearerToken;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.registrationData.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "registration-data-api-test",
	displayName = "Validate structure of all registration data API resources",
	summary = "Validates the structure of all registration data API resources",
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
public class RegistrationDataApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddScopesForRegistrationApi.class);
		callAndStopOnFailure(PrepareAllRegistrationRelatedConsentsForHappyPathTest.class);
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

		runInBlock("Validating personal financial relationship response", () -> {
			callAndStopOnFailure(PrepareToGetPersonalFinancialRelationships.class);
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
