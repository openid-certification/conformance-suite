package net.openid.conformance.openbanking_brasil.testmodules.productsNServices;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.productsNServices.unarrangedAccountOverdraft.UnarrangedAccountBusinessOverdraftValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ProductsNServices Business Unarranged Account Overdraft API test",
	displayName = "Validate structure of ProductsNServices Business Unarranged Account Overdraft Api resources",
	summary = "Validate structure of ProductsNServices Business Unarranged Account Overdraft Api resources",
	profile = OBBProfile.OBB_PROFIlE_PHASE1
)
public class UnarrangedAccountBusinessOverdraftApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate ProductsNServices Business Unarranged Account Overdraft response",
			() -> {
				callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "business-unarranged-account-overdraft");
				preCallResource();
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(UnarrangedAccountBusinessOverdraftValidator.class,
					Condition.ConditionResult.FAILURE);
			});
	}
}
