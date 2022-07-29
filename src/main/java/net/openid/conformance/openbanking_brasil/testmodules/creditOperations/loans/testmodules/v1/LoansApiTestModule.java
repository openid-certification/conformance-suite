package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.testmodules.v1;

import net.openid.conformance.condition.Condition;
import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v1.ContractInstallmentsResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v1.ContractResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v1.ContractGuaranteesResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v1.ContractPaymentsValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v1.GetLoansResponseValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.LoansContractSelector;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.PrepareUrlForFetchingLoanContractInstallmentsResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.PrepareUrlForFetchingLoanContractPaymentsResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.PrepareUrlForFetchingLoanContractResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.PrepareUrlForFetchingLoanContractWarrantiesResource;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "loans-api-test",
	displayName = "Validate structure of all loans API resources",
	summary = "Validates the structure of all loans API resources\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Credit Operations API  (\"LOANS_READ\", \"LOANS_WARRANTIES_READ\", \"LOANS_SCHEDULED_INSTALMENTS_READ\", \"LOANS_PAYMENTS_READ\", \"FINANCINGS_READ\", \"FINANCINGS_WARRANTIES_READ\", \"FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"FINANCINGS_PAYMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\", \"INVOICE_FINANCINGS_READ\", \"INVOICE_FINANCINGS_WARRANTIES_READ\", \"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"INVOICE_FINANCINGS_PAYMENTS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Loans Contracts API\n" +
		"\u2022 Expects 201 - Fetches one of the IDs returned\n" +
		"\u2022 Calls GET Loans Contracts API \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Loans Warranties API \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Loans Contracts Instalments API \n" +
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
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class LoansApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void configureClient(){
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

		runInBlock("Validate loans root response", () -> {
			callAndStopOnFailure(GetLoansResponseValidator.class);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(sequence(ValidateSelfEndpoint.class));
		});

		runInBlock("Validate loans contract response", () -> {
			callAndStopOnFailure(LoansContractSelector.class);
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(ContractResponseValidator.class);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(sequence(ValidateSelfEndpoint.class));
		});

		runInBlock("Validate loans contract warranties response", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractWarrantiesResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(ContractGuaranteesResponseValidator.class);
		});

		runInBlock("Validate loans contract payments response", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractPaymentsResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(ContractPaymentsValidator.class);
		});

		runInBlock("Validate loans contract instalments response", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingLoanContractInstallmentsResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(ContractInstallmentsResponseValidator.class);
		});

	}

}
