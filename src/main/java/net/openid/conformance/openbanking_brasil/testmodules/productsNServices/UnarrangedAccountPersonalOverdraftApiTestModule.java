package net.openid.conformance.openbanking_brasil.testmodules.productsNServices;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.productsNServices.unarrangedAccountOverdraft.UnarrangedAccountPersonalOverdraftValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.LogOnlyFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ProductsNServices ProductsNServices Personal Unarranged Account Overdraft API test",
	displayName = "Validate structure of ProductsNServices Personal Unarranged Account Overdraft Api resources",
	summary = "Validate structure of ProductsNServices Personal Unarranged Account Overdraft Api resources",
	profile = OBBProfile.OBB_PROFIlE_PHASE1
)
public class UnarrangedAccountPersonalOverdraftApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate ProductsNServices Personal Unarranged Account Overdraft response",
			() -> {
				callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class);
				preCallResource();
				callAndContinueOnFailure(DoNotStopOnFailure.class);
				callAndContinueOnFailure(LogOnlyFailure.class);
				callAndContinueOnFailure(UnarrangedAccountPersonalOverdraftValidator.class,
					Condition.ConditionResult.FAILURE);
			});
	}
}
