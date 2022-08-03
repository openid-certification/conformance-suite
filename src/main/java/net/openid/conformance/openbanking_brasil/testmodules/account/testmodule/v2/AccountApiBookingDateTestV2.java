package net.openid.conformance.openbanking_brasil.testmodules.account.testmodule.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.account.v2.AccountIdentificationResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.account.v2.AccountListValidatorV2;
import net.openid.conformance.openbanking_brasil.account.v2.AccountTransactionsValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.account.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

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
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class AccountApiBookingDateTestV2 extends AbstractOBBrasilFunctionalTestModule {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		env.putString("fromBookingDate", currentDate.minusDays(360).format(FORMATTER));
		env.putString("toBookingDate", currentDate.format(FORMATTER));
		callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);
		preCallProtectedResource("Fetch Account transactions");
		callAndStopOnFailure(CopyResourceEndpointResponse.class);
		env.mapKey("full_range_response", "resource_endpoint_response_full_copy");

		eventLog.startBlock("Add booking date query parameters");
		callAndContinueOnFailure(AddBookingDateSixMonthsBefore.class, Condition.ConditionResult.FAILURE);
		preCallProtectedResource("Fetch Account transactions with query parameters");
		eventLog.startBlock("Validating random transaction returned");
		callAndStopOnFailure(CheckExpectedBookingDateResponse.class);
		callAndStopOnFailure(ValidateTransactionWithinRange.class);
		call(accountTransactionsValidationSequence());

		eventLog.startBlock("Add booking date query parameters");
		callAndStopOnFailure(AddBookingDate6MonthsOlderThanCurrent.class);
		preCallProtectedResource("Fetch Account transactions with query parameters");
		eventLog.startBlock("Validating random transaction returned");
		callAndStopOnFailure(CheckExpectedBookingDateResponse.class);
		callAndStopOnFailure(ValidateTransactionWithinRange.class);
		call(accountTransactionsValidationSequence());

		eventLog.startBlock("Add booking date query parameters using value from transaction returned");
		callAndStopOnFailure(AddSavedTransactionDateAsBookingParam.class);
		preCallProtectedResource("Fetch Account transactions with query parameters");
		eventLog.startBlock("Validating random transaction returned");
		callAndStopOnFailure(CheckExpectedBookingDateResponse.class);
		callAndStopOnFailure(ValidateTransactionWithinRange.class);
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
