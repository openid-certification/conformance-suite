package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.testmodules.v2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddOpenIdScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildFinancingsConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.CallProtectedResourceExpectingFailureSequence;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas403;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "financing-api-wrong-permissions-test-v2",
	displayName = "Ensures API resource cannot be called with wrong permissions V2",
	summary = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Credit Operations API  (\"LOANS_READ\", \"LOANS_WARRANTIES_READ\", \"LOANS_SCHEDULED_INSTALMENTS_READ\", \"LOANS_PAYMENTS_READ\", \"FINANCINGS_READ\", \"FINANCINGS_WARRANTIES_READ\", \"FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"FINANCINGS_PAYMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\", \"INVOICE_FINANCINGS_READ\", \"INVOICE_FINANCINGS_WARRANTIES_READ\", \"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"INVOICE_FINANCINGS_PAYMENTS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Financings Contracts API V2\n" +
		"\u2022 Expects 200 - Fetches one of the IDs returned\n" +
		"\u2022 Calls GET Financings Contracts API V2 with ID \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Financings Warranties API V2\n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Financings Payments API V2\n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Financings Contracts Instalments API V2\n" +
		"\u2022 Expects 200\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Customer Personal and Customer Business API (\"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Financings Contracts API V2\n" +
		"\u2022 Expects 403\n" +
		"\u2022 Calls GET Financings Contracts API with ID V2\n" +
		"\u2022 Expects 403\n" +
		"\u2022 Calls GET Financings Warranties API V2\n" +
		"\u2022 Expects 403\n" +
		"\u2022 Calls GET Financings Payments API V2\n" +
		"\u2022 Expects 403\n" +
		"\u2022 Calls GET Financings Contracts Instalments API V2\n" +
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
public class FinancingApiWrongPermissionsTestModuleV2 extends AbstractPermissionsCheckingFunctionalTestModule {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildFinancingsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(AddScopesForFinancingsApi.class);
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
	}

	@Override
	protected void preFetchResources() {
		callAndStopOnFailure(FinancingContractSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingFinancingContractResource.class);
		preCallProtectedResource("Fetch Contracts V2");
		callAndStopOnFailure(PrepareUrlForFetchingFinancingContractInstallmentsResource.class);
		preCallProtectedResource("Fetch Financing Contract list V2");
		callAndStopOnFailure(PrepareUrlForFetchingFinancingContractWarrantiesResource.class);
		preCallProtectedResource("Fetch Warranties V2");
		callAndStopOnFailure(PrepareUrlForFetchingFinancingContractPaymentsResource.class);
		preCallProtectedResource("Fetch Payments V2");
	}

	@Override
	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForFinancingApi.class);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {
		runInBlock("Ensure we cannot call the Financing root API", () -> {
			callAndStopOnFailure(PrepareUrlForFinancingRoot.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the Financing resource API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the financing contract warranties API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractWarrantiesResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the financing contract payments API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractPaymentsResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the  financing contract instalments API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractInstallmentsResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}
