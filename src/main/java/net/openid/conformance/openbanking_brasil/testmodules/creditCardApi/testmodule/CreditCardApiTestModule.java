package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.testmodule;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.creditCard.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.CardBillSelector;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.PrepareUrlForFetchingBillTransactionResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.PrepareUrlForFetchingCardBills;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.PrepareUrlForFetchingCardLimits;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.PrepareUrlForFetchingCardTransactions;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "credit-card-api-test",
	displayName = "Validate structure of all credit card API resources",
	summary = "Validates the structure of all credit card API resources",
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
public class CreditCardApiTestModule extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllCreditCardRelatedConsentsForHappyPathTest.class);
	}

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(CardListResponseResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(CardAccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
		preCallProtectedResource("Fetch Credit Card details");
		callAndContinueOnFailure(CardIdentificationResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(PrepareUrlForFetchingCardLimits.class);
		preCallProtectedResource("Fetch card limits");
		callAndContinueOnFailure(CreditCardAccountsLimitsResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(PrepareUrlForFetchingCardTransactions.class);
		preCallProtectedResource("Fetch card transactions");
		callAndContinueOnFailure(CreditCardAccountsTransactionResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(PrepareUrlForFetchingCardBills.class);
		preCallProtectedResource("Fetch card bills");
		callAndContinueOnFailure(CreditCardBillValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(CardBillSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingBillTransactionResource.class);
		preCallProtectedResource("Fetch Credit Card bill transationc");
		callAndContinueOnFailure(InvoiceCreditCardTransactionsValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));
	}

}
