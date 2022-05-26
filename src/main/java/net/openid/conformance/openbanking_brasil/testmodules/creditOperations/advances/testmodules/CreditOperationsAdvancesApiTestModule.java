package net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.CreditAdvanceSelector;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.PrepareUrlForFetchingCreditAdvanceContractGuarantees;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.PrepareUrlForFetchingCreditAdvanceContractInstallments;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.PrepareUrlForFetchingCreditAdvanceContractPayments;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.PrepareUrlForFetchingCreditAdvanceContracts;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;

import net.openid.conformance.openbanking_brasil.testmodules.support.AddUnarrangedOverdraftScope;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "unarranged-overdraft-api-test",
	displayName = "Validate structure of all unarranged overdraft API resources",
	summary = "Validates the structure of all unarranged overdraft API resources\n" +
		"\u2022 Creates a consent with all the permissions needed to access the Credit Operations API  (\"LOANS_READ\", \"LOANS_WARRANTIES_READ\", \"LOANS_SCHEDULED_INSTALMENTS_READ\", \"LOANS_PAYMENTS_READ\", \"FINANCINGS_READ\", \"FINANCINGS_WARRANTIES_READ\", \"FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"FINANCINGS_PAYMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_WARRANTIES_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_SCHEDULED_INSTALMENTS_READ\", \"UNARRANGED_ACCOUNTS_OVERDRAFT_PAYMENTS_READ\", \"INVOICE_FINANCINGS_READ\", \"INVOICE_FINANCINGS_WARRANTIES_READ\", \"INVOICE_FINANCINGS_SCHEDULED_INSTALMENTS_READ\", \"INVOICE_FINANCINGS_PAYMENTS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects 201 - Expects Success on Redirect - Validates all of the fields sent on the consents API\n" +
		"\u2022 Calls GET Unarranged Overdraft Contracts API\n" +
		"\u2022 Expects 200 - Fetches one of the IDs returned\n" +
		"\u2022 Calls GET Unarranged Overdraft Contracts API with ID \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Unarranged Overdraft Warranties API \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Unarranged Overdraft Payments API \n" +
		"\u2022 Expects 200\n" +
		"\u2022 Calls GET Unarranged Overdraft Contracts Instalments API \n" +
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
		"resource.resourceUrl",
		"consent.productType"
	}
)
public class CreditOperationsAdvancesApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
		callAndStopOnFailure(AddUnarrangedOverdraftScope.class);
	}

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(AdvancesResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(CreditAdvanceSelector.class);

		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContracts.class);
		preCallProtectedResource("Contracts");
		callAndContinueOnFailure(AdvancesContractResponseValidator.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractGuarantees.class);
		preCallProtectedResource("Contract Guarantees");
		callAndContinueOnFailure(AdvancesGuaranteesResponseValidator.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractPayments.class);
		preCallProtectedResource("Contract Payments");
		callAndContinueOnFailure(AdvancesPaymentsResponseValidator.class, Condition.ConditionResult.FAILURE);

		callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractInstallments.class);
		preCallProtectedResource("Contract Installments");
		callAndContinueOnFailure(AdvancesContractInstallmentsResponseValidator.class, Condition.ConditionResult.FAILURE);
	}
}
