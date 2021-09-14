package net.openid.conformance.openbanking_brasil.testmodules.productsNServices;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.productsNServices.accounts.BusinessAccountsValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.accounts.PersonalAccountsValidator;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "ProductsNServicesApi-test",
	displayName = "Validate structure of all ProductsNServices data Api resources",
	summary = "Validate structure of all ProductsNServices data Api resources",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"resource.brazilCpf",
		"resource.resourceUrl",
		"resource.consentUrl"
	}
)
public class ProductsNServicesApiTestModule extends AbstractBrasilFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate ProductsNServices Personal Accounts response", () -> {
		callAndStopOnFailure(PrepareToGetProductsNServices.class, "personal-accounts");
		preCallResource("Personal Accounts");
		callAndStopOnFailure(PersonalAccountsValidator.class);
		});

//		runInBlock("Validate ProductsNServices Business Accounts response", () -> {
//			callAndStopOnFailure(PrepareToGetProductsNServices.class, "business-accounts");
//			preCallResource("Business Accounts");
//			callAndStopOnFailure(BusinessAccountsValidator.class);
//		});

	}
}
