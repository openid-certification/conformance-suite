package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.testmodules.modulesV2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddLoansScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddOpenIdScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallProtectedResourceExpectingFailureSequence;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas403;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "loans-api-wrong-permissions-test-v2",
	displayName = "Ensures API resource cannot be called with wrong permissions",
	summary = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Credit Operations API  (\"LOANS_READ\", \"LOANS_WARRANTIES_READ\", \"LOANS_SCHEDULED_INSTALMENTS_READ\", \"LOANS_PAYMENTS_READ\", \"FINANCINGS_READ\", \"FINANCINGS_WARRANTIES_READ\", \"FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"FINANCINGS_PAYMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\", \"INVOICE_FINANCINGS_READ\", \"INVOICE_FINANCINGS_WARRANTIES_READ\", \"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"INVOICE_FINANCINGS_PAYMENTS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Loans Contracts API - V2\n" +
		"\u2022 Expects 201 - Fetches one of the IDs returned\n" +
		"\u2022 Calls GET Loans Contracts API - V2 \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Loans Payments API - V2 \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Loans Warranties API - V2 \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Loans Contracts Instalments API - V2 \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Customer Personal and Customer Business API (\"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Loans Contracts API - V2\n" +
		"\u2022 Expects 403 \n" +
		"\u2022 Calls GET Loans Contracts API - V2 \n" +
		"\u2022 Expects 403 \n" +
		"\u2022 Calls GET Loans Payments API - V2 \n" +
		"\u2022 Expects 403 \n" +
		"\u2022 Calls GET Loans Warranties API - V2 \n" +
		"\u2022 Expects 403 \n" +
		"\u2022 Calls GET Loans Contracts Instalments API - V2 \n" +
		"\u2022 Expects 403 ",
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
		"resource.resourceUrl"
	}
)
public class LoansApiWrongPermissionsTestModuleV2 extends AbstractPermissionsCheckingFunctionalTestModule {

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(AddLoansScope.class);
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
	}

	@Override
	protected void preFetchResources() {
		callAndStopOnFailure(LoansContractSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingLoanContractResource.class);
		preCallProtectedResource("Fetch Loans Contracts");
		callAndStopOnFailure(PrepareUrlForFetchingLoanContractInstallmentsResource.class);
		preCallProtectedResource("Fetch Loans Contract list");
		callAndStopOnFailure(PrepareUrlForFetchingLoanContractWarrantiesResource.class);
		preCallProtectedResource("Fetch Loans Warranties");
		callAndStopOnFailure(PrepareUrlForFetchingLoanContractPaymentsResource.class);
		preCallProtectedResource("Fetch Loans Payments");
	}

	@Override
	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForLoansApi.class);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {
		runInBlock("Ensure we cannot call the Loans root API", () -> {
			callAndStopOnFailure(PrepareUrlForLoansRoot.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Loans resource API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Loans contract warranties API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractWarrantiesResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Loans contract payments API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractPaymentsResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the  Loans contract instalments API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractInstallmentsResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}
