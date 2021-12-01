package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.common.GetOutagesValidator;
import net.openid.conformance.openbanking_brasil.common.GetStatusValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "common-api-test",
	displayName = "Validate structure of Common API resources",
	summary = "Validates the structure of Common API resources",
	profile = OBBProfile.OBB_PROFIlE_PHASE1
)
public class CommonApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate Common status response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "status");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(GetStatusValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validate Common outages response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "outages");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(GetOutagesValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
