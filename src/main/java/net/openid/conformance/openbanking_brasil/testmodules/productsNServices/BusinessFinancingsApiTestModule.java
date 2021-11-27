package net.openid.conformance.openbanking_brasil.testmodules.productsNServices;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.productsNServices.financings.BusinessFinancingsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ProductsNServices Business Financings API-test",
	displayName = "Validate structure of ProductsNServices Business Financings Api resources",
	summary = "Validate structure of ProductsNServices Business Financings Api resources",
	profile = OBBProfile.OBB_PROFIlE_PHASE1
)
public class BusinessFinancingsApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate ProductsNServices Business Financings response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "business-financings");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(BusinessFinancingsValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
