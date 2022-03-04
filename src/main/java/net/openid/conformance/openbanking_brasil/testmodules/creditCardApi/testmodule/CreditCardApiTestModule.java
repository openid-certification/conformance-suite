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
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.AddScopesForFinancingsApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "credit-card-api-test",
	displayName = "Validate structure of all credit card API resources",
	summary = "Validates the structure of all credit card API resources\n" +
		"\u2022 Creates a Consent with the complete set of the credit cards permission group ([\"CREDIT_CARDS_ACCOUNTS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ\", \"CREDIT_CARDS_ACCOUNTS_LIMITS_READ\", \"CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ\", \"RESOURCES_READ\"])\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Credit Cards Accounts API \n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Credit Cards Accounts API with AccountID specified\n" +
		"\u2022 Expects a 200 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Limits API with AccountID specified\n" +
		"\u2022 Expects a 200 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Transactions API with AccountID specified\n" +
		"\u2022 Expects a 200 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Bills API with AccountID specified\n" +
		"\u2022 Expects a 200 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Bills Transactions API with AccountID specified\n" +
		"\u2022 Expects a 200 response\n",
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
		callAndStopOnFailure(AddCreditCardScopes.class);
	}

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(CardAccountsDataResponseResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(CardAccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
		preCallProtectedResource("Fetch Credit Card details");
		callAndContinueOnFailure(CardIdentificationResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(PrepareUrlForFetchingCardLimits.class);
		preCallProtectedResource("Fetch card limits");
		callAndContinueOnFailure(CreditCardAccountsLimitsResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(PrepareUrlForFetchingCardTransactions.class);
		preCallProtectedResource("Fetch card transactions");
		callAndContinueOnFailure(CreditCardAccountsTransactionResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(PrepareUrlForFetchingCardBills.class);
		preCallProtectedResource("Fetch card bills");
		callAndContinueOnFailure(CreditCardBillValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(CardBillSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingBillTransactionResource.class);
		preCallProtectedResource("Fetch Credit Card bill transaction");
		callAndContinueOnFailure(CreditCardAccountsTransactionBillResponseValidator.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);

		call(sequence(ValidateSelfEndpoint.class));
	}

}
