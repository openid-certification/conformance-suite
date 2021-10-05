package net.openid.conformance.openbanking_brasil.testmodules.productsNServices;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.productsNServices.accounts.BusinessAccountsValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.accounts.PersonalAccountsValidator;

import net.openid.conformance.openbanking_brasil.productsNServices.creditCard.BusinessCreditCardValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.creditCard.PersonalCreditCardValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.financings.BusinessFinancingsValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.financings.PersonalFinancingsValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.invoiceFinancings.BusinessInvoiceFinancingsValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.invoiceFinancings.PersonalInvoiceFinancingsValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.loans.BusinessLoansValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.loans.PersonalLoansValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.unarrangedAccountOverdraft.UnarrangedAccountBusinessOverdraftValidator;
import net.openid.conformance.openbanking_brasil.productsNServices.unarrangedAccountOverdraft.UnarrangedAccountPersonalOverdraftValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractNoAuthFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.DoNotStopOnFailure;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareToGetProductsNChannelsApi;
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
public class ProductsNServicesApiTestModule extends AbstractNoAuthFunctionalTestModule {

	@Override
	protected void runTests() {
		runInBlock("Validate ProductsNServices Personal Accounts response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "personal-accounts");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(PersonalAccountsValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validate ProductsNServices Business Accounts response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "business-accounts");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(BusinessAccountsValidator.class, Condition.ConditionResult.FAILURE);
		});


		runInBlock("Validate ProductsNServices Personal Loans response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "personal-loans");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(PersonalLoansValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validate ProductsNServices Business Loans response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "business-loans");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(BusinessLoansValidator.class, Condition.ConditionResult.FAILURE);
		});


		runInBlock("Validate ProductsNServices Personal Credit Cards response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "personal-credit-cards");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(PersonalCreditCardValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validate ProductsNServices Business Credit Cards response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "business-credit-cards");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(BusinessCreditCardValidator.class, Condition.ConditionResult.FAILURE);
		});


		runInBlock("Validate ProductsNServices Personal Financings response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "personal-financings");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(PersonalFinancingsValidator.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validate ProductsNServices Business Financings response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "business-financings");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(BusinessFinancingsValidator.class, Condition.ConditionResult.FAILURE);
		});


		runInBlock("Validate ProductsNServices Personal Invoice Financings response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "personal-invoice-financings");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(PersonalInvoiceFinancingsValidator.class,
				Condition.ConditionResult.FAILURE);
		});

		runInBlock("Validate ProductsNServices Business Invoice Financings response", () -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "business-invoice-financings");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(BusinessInvoiceFinancingsValidator.class,
				Condition.ConditionResult.FAILURE);
		});


		runInBlock("Validate ProductsNServices Personal Unarranged Account Overdraft response",
			() -> {
			callAndStopOnFailure(PrepareToGetProductsNChannelsApi.class, "personal-unarranged-account-overdraft");
			preCallResource();
			callAndContinueOnFailure(DoNotStopOnFailure.class);
			callAndContinueOnFailure(UnarrangedAccountPersonalOverdraftValidator.class,
				Condition.ConditionResult.FAILURE);
		});

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
