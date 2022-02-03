package net.openid.conformance.openbanking_brasil.testmodules.productsNServices;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.productsNServices.accounts.BusinessAccountsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.LogOnlyFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ProductsNServices - BusinessAccounts API-test",
	displayName = "Validate structure of ProductsNServices - BusinessAccounts API Api resources",
	summary = "Validate structure of ProductsNServices - BusinessAccounts API Api resources",
	profile = OBBProfile.OBB_PROFIlE_PHASE1

)
public class BusinessAccountsApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate ProductsNServices Business Accounts response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class);
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(LogOnlyFailure.class);
			callAndContinueOnFailure(BusinessAccountsValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
