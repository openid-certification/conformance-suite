package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.testmodule.v2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.account.PrepareUrlForFetchingAccountResource;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@PublishTestModule(
	testName = "CreditCard-Api-wrong-permissions-test-v2",
	displayName = "Ensures API resource cannot be called with wrong permissions",
	summary = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test\n" +
		"\u2022 Create a Consent with the complete set of the credit cards permission group ([\"CREDIT_CARDS_ACCOUNTS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_READ\", \"CREDIT_CARDS_ACCOUNTS_BILLS_TRANSACTIONS_READ\", \"CREDIT_CARDS_ACCOUNTS_LIMITS_READ\", \"CREDIT_CARDS_ACCOUNTS_TRANSACTIONS_READ\", \"RESOURCES_READ\"])\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Credit Cards Accounts API V2\n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Credit Cards Transactions API V2 with AccountID specified\n" +
		"\u2022 Expects a 200 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Limits API V2 with AccountID specified\n" +
		"\u2022 Expects a 200 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Transactions API V2 with AccountID specified\n" +
		"\u2022 Expects a 200 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Bills API V2 with AccountID specified\n" +
		"\u2022 Expects a 200 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Bills Transactions API V2 with AccountID specified\n" +
		"\u2022 Expects a 200 response\n" +
		"\u2022 Creates a Consent with customer business and customer personal API resources (\"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Credit Cards Accounts API \n" +
		"\u2022 Expects a 403 response \n" +
		"\u2022 Calls GET Credit Cards Accounts API with AccountID V2 specified\n" +
		"\u2022 Expects a 403 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Limits API with AccountID V2 specified\n" +
		"\u2022 Expects a 403 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Transactions API with AccountID V2 specified\n" +
		"\u2022 Expects a 403 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Bills API with AccountID V2 specified\n" +
		"\u2022 Expects a 403 response\n" +
		"\u2022 Calls GET Credit Cards Accounts Bills Transactions API with AccountID V2 specified\n" +
		"\u2022 Expects a 403 response",
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
public class CreditCardApiWrongPermissionsTestModuleV2 extends AbstractPermissionsCheckingFunctionalTestModule {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildCreditCardsAccountsConfigResourceUrlFromConsentUrl.class);
		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		env.putString("fromBookingDate", currentDate.minusDays(360).format(FORMATTER));
		env.putString("toBookingDate", currentDate.format(FORMATTER));
		super.configureClient();
	}

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(AddOpenIdScope.class);
		callAndStopOnFailure(AddCreditCardScopes.class);
		callAndStopOnFailure(PrepareAllCreditCardRelatedConsentsForHappyPathTest.class);
	}

	@Override
	protected void preFetchResources() {
		callAndStopOnFailure(CardAccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
		preCallProtectedResource("Fetch card bill Account V2");
		callAndStopOnFailure(PrepareUrlForFetchingCardLimits.class);
		preCallProtectedResource("Fetch CreditCard Limits V2");
		callAndStopOnFailure(PrepareUrlForFetchingCardTransactions.class);
		callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);
		preCallProtectedResource("Fetch CreditCard Transactions V2");
		callAndStopOnFailure(PrepareUrlForFetchingCardBills.class);
		preCallProtectedResource("Fetch CreditCard Bills V2");
		callAndStopOnFailure(CardBillSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingBillTransactionResource.class);
		callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);
		preCallProtectedResource("Fetch CreditCard Bills Transaction V2");
	}

	@Override
	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForCreditCardApi.class);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {
		//TODO: need to check why CreditCardRoot not returning 403 on a mock bank
		runInBlock("Ensure we cannot call the CreditCard Root API V2", () -> {
			callAndStopOnFailure(PrepareUrlForCreditCardRoot.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the CreditCard Account API V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the CreditCard Bill API V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCardBills.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the CreditCard Bill Transaction API V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingBillTransactionResource.class);
			callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the CreditCard Limits API V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCardLimits.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the  CreditCard Transactions API V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingCardTransactions.class);
			callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});
	}
}
