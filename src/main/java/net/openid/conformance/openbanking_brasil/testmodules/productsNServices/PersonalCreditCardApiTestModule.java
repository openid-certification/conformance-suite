package net.openid.conformance.openbanking_brasil.testmodules.productsNServices;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.productsNServices.creditCard.PersonalCreditCardValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.LogOnlyFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ProductsNServices Personal Credit Cards API test",
	displayName = "Validate structure of ProductsNServices Personal Credit Cards Api resources",
	summary = "Validate structure of ProductsNServices Personal Credit Cards Api resources",
	profile = OBBProfile.OBB_PROFIlE_PHASE1
)
public class PersonalCreditCardApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate ProductsNServices Personal Credit Cards response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class);
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(LogOnlyFailure.class);
			callAndContinueOnFailure(PersonalCreditCardValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
