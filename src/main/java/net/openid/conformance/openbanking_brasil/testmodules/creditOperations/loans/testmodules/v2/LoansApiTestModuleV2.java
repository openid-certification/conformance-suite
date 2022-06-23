package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.testmodules.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v2.ContractGuaranteesResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v2.ContractInstallmentsResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v2.ContractPaymentsValidatorV2;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v2.ContractResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v2.GetLoansResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.LoansContractSelector;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.PrepareUrlForFetchingLoanContractInstallmentsResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.PrepareUrlForFetchingLoanContractPaymentsResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.PrepareUrlForFetchingLoanContractResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.PrepareUrlForFetchingLoanContractWarrantiesResource;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddLoansScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddOpenIdScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildLoansConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseHasLinks;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateResponseMetaData;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateSelfEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "loans-api-test-v2",
	displayName = "Validate structure of all loans API resources - V2",
	summary = "Validates the structure of all loans API resources - V2\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Credit Operations API  - V2 (\"LOANS_READ\", \"LOANS_WARRANTIES_READ\", \"LOANS_SCHEDULED_INSTALMENTS_READ\", \"LOANS_PAYMENTS_READ\", \"FINANCINGS_READ\", \"FINANCINGS_WARRANTIES_READ\", \"FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"FINANCINGS_PAYMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\", \"INVOICE_FINANCINGS_READ\", \"INVOICE_FINANCINGS_WARRANTIES_READ\", \"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"INVOICE_FINANCINGS_PAYMENTS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Loans Contracts API - V2\n" +
		"\u2022 Expects 201 - Fetches one of the IDs returned\n" +
		"\u2022 Calls GET Loans Contracts API - V2\n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Loans Warranties API - V2\n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Loans Contracts Instalments API - V2 \n" +
		"\u2022 Expects 200",
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
public class LoansApiTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildLoansConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(AddLoansScope.class);
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
	}

	@Override
	protected void validateResponse() {

		runInBlock("Validate loans root response - V2", () -> {
			callAndStopOnFailure(GetLoansResponseValidatorV2.class);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(sequence(ValidateSelfEndpoint.class));
		});

		runInBlock("Validate loans contract response - V2", () -> {
			callAndStopOnFailure(LoansContractSelector.class);
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(ContractResponseValidatorV2.class);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(sequence(ValidateSelfEndpoint.class));
		});

		runInBlock("Validate loans contract warranties response - V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractWarrantiesResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(ContractGuaranteesResponseValidatorV2.class);
		});

		runInBlock("Validate loans contract payments response - V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractPaymentsResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(ContractPaymentsValidatorV2.class);
		});

		runInBlock("Validate loans contract instalments response - V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractInstallmentsResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(ContractInstallmentsResponseValidatorV2.class);
		});

	}

}
