package net.openid.conformance.openbanking_brasil.testmodules.productsNServices.openInsurance;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.productsNServices.openInsurance.GetPensionPlanValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = " ProductsNServices - Pension Plan API test",
	displayName = "Validate structure of ProductsNServices - Pension Plan API Api resources",
	summary = "Validate structure of ProductsNServices - Pension Plan Api resources",
	profile = OBBProfile.OBB_PROFIlE_PHASE1)
public class PensionPlanApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate ProductsNServices - Pension Plan response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "pension-plan");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(GetPensionPlanValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
