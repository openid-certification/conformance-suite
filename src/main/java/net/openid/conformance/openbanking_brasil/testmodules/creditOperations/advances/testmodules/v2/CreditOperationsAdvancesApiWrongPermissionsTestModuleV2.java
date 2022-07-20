package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.testmodules.v2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddUnarrangedOverdraftScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildCreditOperationsAdvancesConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallProtectedResourceExpectingFailureSequence;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas403;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "unarranged-overdraft-api-wrong-permissions-test-v2",
	displayName = "Ensures API resource cannot be called with wrong permissions",
	summary = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Credit Operations API  (\"LOANS_READ\", \"LOANS_WARRANTIES_READ\", \"LOANS_SCHEDULED_INSTALMENTS_READ\", \"LOANS_PAYMENTS_READ\", \"FINANCINGS_READ\", \"FINANCINGS_WARRANTIES_READ\", \"FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"FINANCINGS_PAYMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\", \"INVOICE_FINANCINGS_READ\", \"INVOICE_FINANCINGS_WARRANTIES_READ\", \"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"INVOICE_FINANCINGS_PAYMENTS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Unarranged Overdraft Contracts API V2\n" +
		"\u2022 Expects 200 - Fetches one of the IDs returned\n" +
		"\u2022 Calls GET Unarranged Overdraft Contracts API with ID V2\n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Unarranged Overdraft Warranties API V2\n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Unarranged Overdraft Payments API V2\n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Unarranged Overdraft Contracts Instalments API V2\n" +
		"\u2022 Expects 200\n" +
		"\u2022 reates a consent with all the permissions needed to access the Customer Personal or the Customer Business API (\"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\",  \"RESOURCES_READ\")or (“CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ”,” \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ”, \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Unarranged Overdraft Contracts API V2\n" +
		"\u2022 Expects 403\n" +
		"\u2022 Calls GET Unarranged Overdraft Contracts API with ID V2\n" +
		"\u2022 Expects 403\n" +
		"\u2022 Calls GET Unarranged Overdraft Warranties API V2\n" +
		"\u2022 Expects 403\n" +
		"\u2022 Calls GET Unarranged Overdraft Payments API V2\n" +
		"\u2022 Expects 403\n" +
		"\u2022 Calls GET Unarranged Overdraft Contracts Instalments API V2\n" +
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
		"resource.brazilCpf"
	}
)
public class CreditOperationsAdvancesApiWrongPermissionsTestModuleV2 extends AbstractPermissionsCheckingFunctionalTestModule {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildCreditOperationsAdvancesConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
		callAndStopOnFailure(AddUnarrangedOverdraftScope.class);
	}

	@Override
	protected void preFetchResources() {
		callAndStopOnFailure(CreditAdvanceSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContracts.class);
		preCallProtectedResource("Fetch Loans Contracts V2");
		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractInstallments.class);
		preCallProtectedResource("Fetch Loans Contract list V2");
		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractGuarantees.class);
		preCallProtectedResource("Fetch Loans Warranties V2");
		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractPayments.class);
		preCallProtectedResource("Fetch Loans Payments V2");
	}

	@Override
	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForAdvancesApi.class);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {
		runInBlock("Ensure we cannot call the CreditOperation Advances root API V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceRoot.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the CreditOperation Advances Contracts API V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContracts.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the CreditOperation Advances warranties API V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractGuarantees.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the CreditOperation Advances payments API V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractPayments.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the  CreditOperation Advances instalments API V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractInstallments.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}
