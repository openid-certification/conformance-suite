package net.openid.conformance.openbanking_brasil.testmodules.productsNServices;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.productsNServices.creditCard.BusinessCreditCardValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = " ProductsNServices Business Credit Cards API test",
	displayName = "Validate structure of ProductsNServices Business Credit Cards API Api resources",
	summary = "Validate structure of ProductsNServices Business Credit Cards Api resources",
	profile = OBBProfile.OBB_PROFIlE_PHASE1
)
public class BusinessCreditCardApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate ProductsNServices Business Credit Cards response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class);
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(BusinessCreditCardValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
