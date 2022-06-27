package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.testmodules.v1;

import net.openid.conformance.condition.Condition;
import com.google.gson.JsonObject;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.v1.FinancingContractInstallmentsResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.v1.FinancingContractResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.v1.FinancingGuaranteesResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.v1.FinancingPaymentsResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.v1.FinancingResponseValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.AddScopesForFinancingsApi;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.FinancingContractSelector;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.PrepareUrlForFetchingFinancingContractInstallmentsResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.PrepareUrlForFetchingFinancingContractPaymentsResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.PrepareUrlForFetchingFinancingContractResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.PrepareUrlForFetchingFinancingContractWarrantiesResource;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "financings-api-test",
	displayName = "Validate structure of all financing API resources",
	summary = "Validates the structure of all financing API resources\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Credit Operations API  (\"LOANS_READ\", \"LOANS_WARRANTIES_READ\", \"LOANS_SCHEDULED_INSTALMENTS_READ\", \"LOANS_PAYMENTS_READ\", \"FINANCINGS_READ\", \"FINANCINGS_WARRANTIES_READ\", \"FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"FINANCINGS_PAYMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\", \"INVOICE_FINANCINGS_READ\", \"INVOICE_FINANCINGS_WARRANTIES_READ\", \"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"INVOICE_FINANCINGS_PAYMENTS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Financings Contracts API\n" +
		"\u2022 Expects 200 - Fetches one of the IDs returned\n" +
		"\u2022 Calls GET Financings Contracts API with ID \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Financings Warranties API \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Financings Payments API \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Financings Contracts Instalments API \n" +
		"\u2022 Expects 200\n",
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
public class FinancingsApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildFinancingsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(AddScopesForFinancingsApi.class);
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
	}

	@Override
	protected void validateResponse() {

		runInBlock("Validate financing root response", () -> {
			callAndStopOnFailure(FinancingResponseValidator.class);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(sequence(ValidateSelfEndpoint.class));
		});

		runInBlock("Validate financing contract response", () -> {
			callAndStopOnFailure(FinancingContractSelector.class);
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(FinancingContractResponseValidator.class);
			callAndStopOnFailure(LogKnownIssue.class,"BCLOG-F02-173","BCLOG-F02-175");
		});

		runInBlock("Validate financing contract warranties response", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractWarrantiesResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(FinancingGuaranteesResponseValidator.class);
		});

		runInBlock("Validate financing contract payments response", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractPaymentsResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(FinancingPaymentsResponseValidator.class);
			callAndStopOnFailure(EnsureResponseHasLinks.class);
			callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
			call(sequence(ValidateSelfEndpoint.class));
		});

		runInBlock("Validate financing contract instalments response", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractInstallmentsResource.class);
			preCallProtectedResource();
			callAndStopOnFailure(FinancingContractInstallmentsResponseValidator.class);
			callAndStopOnFailure(LogKnownIssue.class,"BCLOG-F02-174","BCLOG-F02-176");
		});

	}

}
