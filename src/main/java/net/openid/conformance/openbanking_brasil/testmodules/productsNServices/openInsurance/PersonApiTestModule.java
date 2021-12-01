package net.openid.conformance.openbanking_brasil.testmodules.productsNServices.openInsurance;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.productsNServices.openInsurance.GetPersonValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = " ProductsNServices - Person API test",
	displayName = "Validate structure of ProductsNServices - Person API Api resources",
	summary = "Validate structure of ProductsNServices - Person Api resources",
	profile = OBBProfile.OBB_PROFIlE_PHASE1)
public class PersonApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate ProductsNServices - Person response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "person");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(GetPersonValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
