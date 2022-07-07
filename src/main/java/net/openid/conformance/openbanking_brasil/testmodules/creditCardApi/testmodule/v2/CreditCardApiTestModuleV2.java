package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.testmodule.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CardAccountsDataResponseResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CardIdentificationResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CreditCardAccountsLimitsResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CreditCardAccountsTransactionBillResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CreditCardAccountsTransactionResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CreditCardBillValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "credit-card-api-test-v2",
	displayName = "Validate structure of all credit card API resources",
	summary = "Validates the structure of all credit card API resources\n" +
		"\u2022 Creates a Consent with the complete set of the credit cards permission group ([\"CREDIT_CARDS_ACCOUNTS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ\", \"CREDIT_CARDS_ACCOUNTS_LIMITS_READ\", \"CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ\", \"RESOURCES_READ\"])\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Credit Cards Accounts API V2\n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Credit Cards Accounts API V2 with AccountID specified\n" +
		"\u2022 Expects a 200 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Limits API V2 with AccountID specified\n" +
		"\u2022 Expects a 200 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Transactions API V2 with AccountID specified\n" +
		"\u2022 Expects a 200 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Bills API V2 with AccountID specified\n" +
		"\u2022 Expects a 200 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Bills Transactions V2 API with AccountID specified\n" +
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
	}
)
public class CreditCardApiTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildCreditCardsAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllCreditCardRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(AddCreditCardScopes.class);
	}

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(CardAccountsDataResponseResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(CardAccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
		preCallProtectedResource("Fetch Credit Card details V2");
		callAndContinueOnFailure(CardIdentificationResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(PrepareUrlForFetchingCardLimits.class);
		preCallProtectedResource("Fetch card limits V2");
		callAndContinueOnFailure(CreditCardAccountsLimitsResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(PrepareUrlForFetchingCardTransactions.class);
		preCallProtectedResource("Fetch card transactions V2");
		callAndContinueOnFailure(CreditCardAccountsTransactionResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateMetaOnlyRequestDateTime.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(PrepareUrlForFetchingCardBills.class);
		preCallProtectedResource("Fetch card bills V2");
		callAndContinueOnFailure(CreditCardBillValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		call(sequence(ValidateSelfEndpoint.class));

		callAndStopOnFailure(CardBillSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingBillTransactionResource.class);
		preCallProtectedResource("Fetch Credit Card bill transaction V2");
		callAndContinueOnFailure(CreditCardAccountsTransactionBillResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateMetaOnlyRequestDateTime.class, Condition.ConditionResult.FAILURE);

		call(sequence(ValidateSelfEndpoint.class));
	}

}
