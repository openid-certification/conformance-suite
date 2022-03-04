package net.openid.conformance.openbanking_brasil.testmodules.customerAPI.testmodule;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.registrationData.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareAllCustomerBusinessRelatedConsentsForHappyPathTest;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetBusinessFinancialRelations;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetBusinessIdentifications;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.PrepareToGetBusinessQualifications;
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
@CallProtectedResource.FixMe
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
			// TODO use CallProtectedResource
//			callAndContinueOnFailure(CallProtectedResourceWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(BusinessRelationsResponseValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating business identifications response", () -> {
			callAndStopOnFailure(PrepareToGetBusinessIdentifications.class);
			// TODO use CallProtectedResource
//			callAndContinueOnFailure(CallProtectedResourceWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(BusinessIdentificationValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validating business qualifications response", () -> {
			callAndStopOnFailure(PrepareToGetBusinessQualifications.class);
			// TODO use CallProtectedResource
//			callAndContinueOnFailure(CallProtectedResourceWithBearerToken.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(BusinessQualificationResponseValidator.class, Condition.ConditionResult.FAILURE);
		});

	}
}
