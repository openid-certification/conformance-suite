package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.testmodule;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.*;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.CreditDiscountedCreditRightsSelector;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.PrepareUrlForDiscountedRoot;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.PrepareUrlForFetchingCreditDiscountedCreditRightsContract;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.PrepareUrlForFetchingCreditDiscountedCreditRightsContractGuarantees;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.PrepareUrlForFetchingCreditDiscountedCreditRightsContractInstalments;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.PrepareUrlForFetchingCreditDiscountedCreditRightsContractPayments;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.condition.Condition;

@PublishTestModule(
	testName = "discounted-credit-rights-wrong-permissions-test",
	displayName = "Ensures API resource cannot be called with wrong permissions",
	summary = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Credit Operations API  (\"LOANS_READ\", \"LOANS_WARRANTIES_READ\", \"LOANS_SCHEDULED_INSTALMENTS_READ\", \"LOANS_PAYMENTS_READ\", \"FINANCINGS_READ\", \"FINANCINGS_WARRANTIES_READ\", \"FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"FINANCINGS_PAYMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\", \"INVOICE_FINANCINGS_READ\", \"INVOICE_FINANCINGS_WARRANTIES_READ\", \"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"INVOICE_FINANCINGS_PAYMENTS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Discounted Credit Rights  Contracts API\n" +
		"\u2022 Expects 200 - Fetches one of the IDs returned\n" +
		"\u2022 Calls GET Discounted Credit Rights  Contracts API with ID \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Discounted Credit Rights  Warranties API \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Discounted Credit Rights  Payments API \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Discounted Credit Rights  Contracts Instalments API \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Customer Personal and Customer Business API (\"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Discounted Credit Rights  Contracts API\n" +
		"\u2022 Expects 403\n" +
		"\u2022 Calls GET Discounted Credit Rights  Contracts API with ID \n" +
		"\u2022 Expects 403\n" +
		"\u2022 Calls GET Discounted Credit Rights  Warranties API \n" +
		"\u2022 Expects 403\n" +
		"\u2022 Calls GET Discounted Credit Rights  Payments API \n" +
		"\u2022 Expects 403\n" +
		"\u2022 Calls GET Discounted Credit Rights  Contracts Instalments API \n" +
		"\u2022 Expects 403",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"consent.productType"
	}
)
public class CreditOperationsDiscountedApiWrongPermissionsTestModule extends AbstractPermissionsCheckingFunctionalTestModule {

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildCreditOperationsDiscountedConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
	}

	@Override
	protected void preFetchResources() {
		callAndStopOnFailure(CreditDiscountedCreditRightsSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContract.class);
		preCallProtectedResource("Discounted Credit Rights - Contract");
		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractGuarantees.class);
		preCallProtectedResource("Discounted Credit Rights - Contract Guarantees");
		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractPayments.class);
		preCallProtectedResource("Discounted Credit Rights - Contract Payments");
		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractInstalments.class);
		preCallProtectedResource("Discounted Credit Rights - Contract Instalments");
	}

	@Override
	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForCreditCardApi.class);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {
		runInBlock("Ensure we cannot call the Discounted Credit Rights root", () -> {
			callAndStopOnFailure(PrepareUrlForDiscountedRoot.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Discounted Credit Rights - Contract", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContract.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Discounted Credit Rights - Contract Guarantees", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractGuarantees.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Discounted Credit Rights - Contract Payments", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractPayments.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Discounted Credit Rights - Contract Instalments", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractInstalments.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}
