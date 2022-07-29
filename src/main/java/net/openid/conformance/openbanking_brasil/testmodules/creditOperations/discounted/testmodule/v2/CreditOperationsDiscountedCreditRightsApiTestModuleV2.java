package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.testmodule.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.v2.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddInvoiceFinancingsScope;
import net.openid.conformance.openbanking_brasil.testmodules.support.BuildCreditOperationsDiscountedConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseHasLinks;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateResponseMetaData;
import net.openid.conformance.openbanking_brasil.testmodules.support.ValidateSelfEndpoint;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "discounted-credit-rights-api-test-v2",
	displayName = "Validate structure of all discounted credit rights API resources V2",
	summary = "Validates the structure of all discounted credit rights API resources V2\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Credit Operations API  (\"LOANS_READ\", \"LOANS_WARRANTIES_READ\", \"LOANS_SCHEDULED_INSTALMENTS_READ\", \"LOANS_PAYMENTS_READ\", \"FINANCINGS_READ\", \"FINANCINGS_WARRANTIES_READ\", \"FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"FINANCINGS_PAYMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\", \"INVOICE_FINANCINGS_READ\", \"INVOICE_FINANCINGS_WARRANTIES_READ\", \"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"INVOICE_FINANCINGS_PAYMENTS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Discounted Credit Rights  Contracts API V2\n" +
		"\u2022 Expects 200 - Fetches one of the IDs returned\n" +
		"\u2022 Calls GET Discounted Credit Rights  Contracts API with ID V2\n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Discounted Credit Rights  Warranties API V2\n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Discounted Credit Rights  Payments API V2\n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Discounted Credit Rights  Contracts Instalments API V2\n" +
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
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class CreditOperationsDiscountedCreditRightsApiTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void configureClient() {
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
		callAndContinueOnFailure(InvoiceFinancingContractsResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(CreditDiscountedCreditRightsSelector.class);

		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContract.class);
		preCallProtectedResource("Discounted Credit Rights - Contract V2");
		callAndContinueOnFailure(InvoiceFinancingAgreementResponseValidatorV2.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractGuarantees.class);
		preCallProtectedResource("Discounted Credit Rights - Contract Guarantees V2");
		callAndContinueOnFailure(InvoiceFinancingContractGuaranteesResponseValidatorV2.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractPayments.class);
		preCallProtectedResource("Discounted Credit Rights - Contract Payments V2");
		callAndContinueOnFailure(InvoiceFinancingContractPaymentsResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractInstalments.class);
		preCallProtectedResource("Discounted Credit Rights - Contract Instalments V2");
		callAndContinueOnFailure(InvoiceFinancingContractInstallmentsResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
	}
}
