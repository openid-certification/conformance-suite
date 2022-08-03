package net.openid.conformance.openbanking_brasil.testmodules.account.testmodule.v1;

import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractPermissionsCheckingFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.account.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.condition.Condition;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@PublishTestModule(
	testName = "account-api-wrong-permissions-test",
	displayName = "Ensures API resource cannot be called with wrong permissions",
	summary = "Ensures API resource cannot be called with wrong permissions - there will be two browser interactions with this test\n" +
		"\u2022 Creates a Consent with the accounts permission group (\"ACCOUNTS_READ\", \"ACCOUNTS_BALANCES_READ\", \"RESOURCES_READ\", \"ACCOUNTS_TRANSACTIONS_READ\", \"ACCOUNTS_OVERDRAFT_LIMITS_READ\")\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Accounts API \n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Accounts API specifying an account ID\n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Accounts Balances API specifying an account ID\n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Accounts Limits API specifying an account ID\n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Calls GET Accounts Transactions API specifying an account ID\n" +
		"\u2022 Expects a 200 response \n" +
		"\u2022 Creates a Consent with the customer business and personal permissions group (\"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"CUSTOMERS_PERSONAL_IDENTIFICATIONS_READ\", \"CUSTOMERS_BUSINESS_IDENTIFICATIONS_READ\", \"RESOURCES_READ\")\n" +
		"\u2022 Expects a success 201 - Expects a success on Redirect as well \n" +
		"\u2022 Calls GET Accounts API \n" +
		"\u2022 Expects a 403 response \n" +
		"\u2022 Calls GET Accounts API specifying an account ID\n" +
		"\u2022 Expects a 403 response \n" +
		"\u2022 Calls GET Accounts Balances API specifying an account ID\n" +
		"\u2022 Expects a 403 response \n" +
		"\u2022 Calls GET Accounts Limits API specifying an account ID\n" +
		"\u2022 Expects a 403 response \n" +
		"\u2022 Calls GET Accounts Transactions API specifying an account ID\n" +
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
public class AccountsApiWrongPermissionsTestModule extends AbstractPermissionsCheckingFunctionalTestModule {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		env.putString("fromBookingDate", currentDate.minusDays(360).format(FORMATTER));
		env.putString("toBookingDate", currentDate.format(FORMATTER));
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

		preCallProtectedResource("Fetch Account");
		callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);
		preCallProtectedResource("Fetch Account balance");
		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);
		callAndStopOnFailure(AddToAndFromBookingDateParametersToProtectedResourceUrl.class);
		preCallProtectedResource("Fetch Account transactions");
		callAndStopOnFailure(PrepareUrlForFetchingAccountLimits.class);
		preCallProtectedResource("Fetch Account limits");

	}

	@Override
	protected void prepareIncorrectPermissions() {
		callAndStopOnFailure(ProvideIncorrectPermissionsForAccountsApi.class);
	}

	@Override
	protected void requestResourcesWithIncorrectPermissions() {

		runInBlock("Ensure we cannot call the accounts root API", () -> {
			callAndStopOnFailure(PrepareUrlForAccountsRoot.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the account resource API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the account balance API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the account transactions API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);
			callAndStopOnFailure(AddToAndFromBookingDateParametersToProtectedResourceUrl.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

		runInBlock("Ensure we cannot call the account limits API", () -> {
			callAndStopOnFailure(PrepareUrlForFetchingAccountLimits.class);
			call(sequence(CallProtectedResourceExpectingFailureSequence.class));
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas403.class);
		});

	}

}
