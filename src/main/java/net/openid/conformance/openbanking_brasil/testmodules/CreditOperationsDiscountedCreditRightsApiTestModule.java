package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.CreditDiscountedCreditRightsSelector;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareAllCreditDiscountedCreditRightsRelatedConsentsForHappyPathTest;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareUrlForFetchingCreditDiscountedCreditRightsContract;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareUrlForFetchingCreditDiscountedCreditRightsContractGuarantees;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareUrlForFetchingCreditDiscountedCreditRightsContractPayments;
import net.openid.conformance.openbanking_brasil.testmodules.support.PrepareUrlForFetchingCreditDiscountedCreditRightsContractInstalments;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "discounted-credit-rights-api-test",
	displayName = "Validate structure of all discounted credit rights API resources",
	summary = "Validates the structure of all discounted credit rights API resources",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.resourceUrl"
	}
)
public class CreditOperationsDiscountedCreditRightsApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllCreditDiscountedCreditRightsRelatedConsentsForHappyPathTest.class);
	}

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(InvoiceFinancingContractsResponseValidator.class, Condition.ConditionResult.FAILURE);

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

		callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractInstalments.class);
		preCallProtectedResource("Discounted Credit Rights - Contract Instalments");
		callAndContinueOnFailure(InvoiceFinancingContractInstallmentsResponseValidator.class, Condition.ConditionResult.FAILURE);
	}
}
