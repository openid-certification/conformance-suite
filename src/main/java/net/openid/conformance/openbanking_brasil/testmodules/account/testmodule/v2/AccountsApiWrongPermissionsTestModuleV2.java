package net.openid.conformance.openbanking_brasil.testmodules.account.testmodule.v2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.account.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "account-api-wrong-permissions-test-v2",
	displayName = "Ensures API resource cannot be called with wrong permissions",
	summary = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test\n" +
		"\u2022 Creates a Consent with the accounts permission group (\"ACCOUNTS_READ\", \"ACCOUNTS_BALANCES_READ\", \"RESOURCES_READ\", \"ACCOUNTS_TRANSACTIONS_READ\", \"ACCOUNTS_OVERDRAFT_LIMITS_READ\")\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Accounts API V2\n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Accounts API V2 specifying an account ID\n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Accounts Balances API V2 specifying an account ID\n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Accounts Limits API V2 specifying an account ID\n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Accounts Transactions API V2 specifying an account ID\n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Creates a Consent with the customer business and personal permissions group (\"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Accounts API V2\n" +
		"\u2022 Expects a 403 response \n" +
		"\u2022 Calls GET Accounts API V2 specifying an account ID\n" +
		"\u2022 Expects a 403 response \n" +
		"\u2022 Calls GET Accounts Balances API V2 specifying an account ID\n" +
		"\u2022 Expects a 403 response \n" +
		"\u2022 Calls GET Accounts Limits API V2 specifying an account ID\n" +
		"\u2022 Expects a 403 response \n" +
		"\u2022 Calls GET Accounts Transactions API V2 specifying an account ID\n" +
		"\u2022 Expects a 403 response ",
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
public class AccountsApiWrongPermissionsTestModuleV2 extends AbstractPermissionsCheckingFunctionalTestModule {
	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void prepareCorrectConsents() {
		callAndStopOnFailure(AddAccountScope.class);
		callAndStopOnFailure(PrepareAllAccountRelatedConsentsForHappyPathTest.class);
	}

	@Override
	protected void preFetchResources() {

		callAndStopOnFailure(AccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);

		preCallProtectedResource("Fetch Account V2");
		callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);
		preCallProtectedResource("Fetch Account balance V2");
		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);
		callAndStopOnFailure(AddBookingDateOneYearBefore.class);
		preCallProtectedResource("Fetch Account transactions V2");
		callAndStopOnFailure(PrepareUrlForFetchingAccountLimits.class);
		preCallProtectedResource("Fetch Account limits V2");

	}

	@Override
	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForAccountsApi.class);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {

		runInBlock("Ensure we cannot call the accounts root API V2", () -> {
			callAndStopOnFailure(PrepareUrlForAccountsRoot.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the account resource API V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the account balance API V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the account transactions API V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);
			callAndStopOnFailure(AddBookingDateOneYearBefore.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the account limits API V2", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountLimits.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

	}

}
