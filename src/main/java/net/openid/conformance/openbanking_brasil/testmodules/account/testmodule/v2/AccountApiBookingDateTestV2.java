package net.openid.conformance.openbanking_brasil.testmodules.account.testmodule.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.testmodules.account.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "account-api-bookingdate-test-v2",
	displayName = "Test the max date of a payment",
	summary = "Testing that the server is respecting the BookingDate filter rules\n" +
		"\u2022 Creates a consent with only ACCOUNTS permissions\n" +
		"\u2022 201 code and successful redirect\n" +
		"\u2022 Using the consent created, call the Accounts API\n" +
		"\u2022 Call GET Accounts Transactions API, send query parameters fromBookingDate and toBookingDate using 6 months before current date (From D to D-180)\n" +
		"\u2022 Expect success, fetch a transaction, get the transactionDate, make sure this transaction is within the range above\n" +
		"\u2022 Call GET Accounts Transactions API, send query parameters fromBookingDate and toBookingDate using 6 months older than current date (From D-180 to D-360)\n" +
		"\u2022 Expect success, fetch a transaction, get the transactionDate, make sure this transaction is within the range above - Save the date from one of the transactions returned on that API Call - Save it's date\n" +
		"\u2022 Call GET Accounts Transactions API, send query parameters fromBookingDate and toBookingDate to be the transactionDate saved on the test below\n" +
		"\u2022 Expect success, make sure that the returned transactions is from exactly the date returned above\n",
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

public class AccountApiBookingDateTestV2 extends AbstractOBBrasilFunctionalTestModule {
	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddAccountScope.class);
		callAndStopOnFailure(PrepareAllAccountRelatedConsentsForHappyPathTest.class);
	}
	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(AccountListValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(AccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
		preCallProtectedResource("Fetch Account");
		callAndContinueOnFailure(AccountIdentificationResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);
		preCallProtectedResource("Fetch Account transactions");
		eventLog.startBlock("Add booking date query parameters");
		callAndContinueOnFailure(AddBookingDateSixMonthsBefore.class, Condition.ConditionResult.FAILURE);
		preCallProtectedResource("Fetch Account transactions with query parameters");
		eventLog.startBlock("Validating random transaction returned");
		callAndStopOnFailure(validateTransactionWithinRange.class);
		call(accountTransactionsValidationSequence());
		eventLog.startBlock("Add booking date query parameters");
		callAndStopOnFailure(AddBookingDate6MonthsOlderThanCurrent.class);
		preCallProtectedResource("Fetch Account transactions with query parameters");
		eventLog.startBlock("Validating random transaction returned");
		callAndStopOnFailure(validateTransactionWithinRange.class);
		call(accountTransactionsValidationSequence());
		eventLog.startBlock("Add booking date query parameters using value from transaction returned");
		callAndStopOnFailure(AddSavedTransactionDateAsBookingParam.class);
		preCallProtectedResource("Fetch Account transactions with query parameters");
		eventLog.startBlock("Validating random transaction returned");
		callAndStopOnFailure(validateTransactionWithinRange.class);
		call(accountTransactionsValidationSequence());
	}

	private ConditionSequence accountTransactionsValidationSequence(){
		return sequenceOf(
			condition(AccountTransactionsValidatorV2.class),
			condition(EnsureResponseHasLinks.class),
			condition(ValidateResponseMetaData.class),
			sequence(ValidateSelfEndpoint.class)
		);
	}
}
