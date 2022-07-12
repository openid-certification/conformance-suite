package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.testmodule.v1;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.v1.InvoiceFinancingAgreementResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.v1.InvoiceFinancingContractGuaranteesResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.v1.InvoiceFinancingContractInstallmentsResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.v1.InvoiceFinancingContractPaymentsResponseValidator;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.v1.InvoiceFinancingContractsResponseValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.CreditDiscountedCreditRightsSelector;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.PrepareUrlForFetchingCreditDiscountedCreditRightsContract;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.PrepareUrlForFetchingCreditDiscountedCreditRightsContractGuarantees;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.PrepareUrlForFetchingCreditDiscountedCreditRightsContractInstalments;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.PrepareUrlForFetchingCreditDiscountedCreditRightsContractPayments;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;

@PublishTestModule(
	testName = "discounted-credit-rights-api-test",
	displayName = "Validate structure of all discounted credit rights API resources",
	summary = "Validates the structure of all discounted credit rights API resources\n" +
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
		"resource.brazilCpf"
	}
)
public class CreditOperationsDiscountedCreditRightsApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildCreditOperationsDiscountedConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
		callAndStopOnFailure(AddInvoiceFinancingsScope.class);
	}

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(InvoiceFinancingContractsResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(CreditDiscountedCreditRightsSelector.class);

		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContract.class);
		preCallProtectedResource("Discounted Credit Rights - Contract");
		callAndContinueOnFailure(InvoiceFinancingAgreementResponseValidator.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractGuarantees.class);
		preCallProtectedResource("Discounted Credit Rights - Contract Guarantees");
		callAndContinueOnFailure(InvoiceFinancingContractGuaranteesResponseValidator.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractPayments.class);
		preCallProtectedResource("Discounted Credit Rights - Contract Payments");
		callAndContinueOnFailure(InvoiceFinancingContractPaymentsResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractInstalments.class);
		preCallProtectedResource("Discounted Credit Rights - Contract Instalments");
		callAndContinueOnFailure(InvoiceFinancingContractInstallmentsResponseValidator.class, Condition.ConditionResult.FAILURE);
	}
}