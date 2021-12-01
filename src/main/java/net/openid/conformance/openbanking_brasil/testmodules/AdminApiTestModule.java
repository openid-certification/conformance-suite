package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.admin.GetMetricsAdminApiValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "admin-api-test",
	displayName = "Validate structure of admin API resources",
	summary = "Validates the structure of admin API resources",
	profile = OBBProfile.OBB_PROFILE
)
public class AdminApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate Admin Metrics response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "metrics");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(GetMetricsAdminApiValidator.class, Condition.ConditionResult.FAILURE);
		});


	}
	}
