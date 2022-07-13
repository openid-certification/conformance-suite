package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.*;
import net.openid.conformance.openbanking_brasil.account.v1.AccountIdentificationResponseValidator;
import net.openid.conformance.openbanking_brasil.account.v1.AccountListValidator;
import net.openid.conformance.openbanking_brasil.account.v1.AccountTransactionsValidator;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.util.JsonUtils;
import org.openqa.selenium.json.Json;

@PublishTestModule(
	testName = "account-api-bookingdate-test",
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

public class AccountApiBookingDateTest extends AbstractOBBrasilFunctionalTestModule{
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
//		callAndContinueOnFailure(AccountListValidator.class, Condition.ConditionResult.FAILURE);
//		callAndStopOnFailure(AccountSelector.class);
//		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);
//		preCallProtectedResource("Fetch Account");
//		callAndContinueOnFailure(AccountIdentificationResponseValidator.class, Condition.ConditionResult.FAILURE);
//		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);
//		preCallProtectedResource("Fetch Account transactions");
		eventLog.startBlock("Add booking date query parameters");
		env.putString("accountId", "Test");
		callAndContinueOnFailure(AddBookingDateSixMonthsBefore.class, Condition.ConditionResult.FAILURE);
		preCallProtectedResource("Fetch Account transactions with query parameters");

		JsonObject body = JsonUtils.createBigDecimalAwareGson().fromJson("{\"data\":[{\"transactionId\":\"27218640\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":13,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-06-24\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27220969\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":13,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27222649\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":13,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27223029\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":13,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27223064\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":13,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27161336\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":6.5,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27161337\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":6.5,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27161338\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":6.5,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27161564\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":10,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27162267\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":13,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27162575\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":13,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27163111\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":13,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27163879\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":6.5,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27163980\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"CREDITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":100000,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27163981\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":2.2,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27163982\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":2.2,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27164742\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":13,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27164899\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":13,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27164975\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":6.5,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27167268\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":6.5,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27167827\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":6.5,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27168507\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":13,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27169999\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":13,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27172319\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":13,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"},{\"transactionId\":\"27282858\",\"completedAuthorisedPaymentType\":\"TRANSACAO_EFETIVADA\",\"creditDebitType\":\"DEBITO\",\"transactionName\":\"COMPRA CARTÃO ORIGEMY \",\"type\":\"CARTAO\",\"amount\":9.75,\"transactionCurrency\":\"BRL\",\"transactionDate\":\"2022-07-11\",\"partieCnpjCpf\":\"NA\",\"partiePersonType\":\"PESSOA_NATURAL\",\"partieCompeCode\":\"NA\",\"partieBranchCode\":\"0000\",\"partieNumber\":\"00000000\",\"partieCheckDigit\":\"0\"}],\"links\":{\"self\":\"https://api-sandbox.original.com.br/open-banking/accounts/v1/accounts/00092083/transactions?fromBookingDate=2022-01-12&toBookingDate=2022-07-11&page=1&page-size=25\",\"first\":\"https://api-sandbox.original.com.br/open-banking/accounts/v1/accounts/00092083/transactions?fromBookingDate=2022-01-12&toBookingDate=2022-07-11&page=1&page-size=25\",\"next\":\"https://api-sandbox.original.com.br/open-banking/accounts/v1/accounts/00092083/transactions?fromBookingDate=2022-01-12&toBookingDate=2022-07-11&page=2&page-size=25\",\"last\":\"https://api-sandbox.original.com.br/open-banking/accounts/v1/accounts/00092083/transactions?fromBookingDate=2022-01-12&toBookingDate=2022-07-11&page=4&page-size=25\"},\"meta\":{\"totalRecords\":76,\"totalPages\":4,\"requestDateTime\":\"2022-07-11T20:18:07Z\"}}", JsonObject.class);
		env.putObject("resource_endpoint_response_full", "body", body);


		eventLog.startBlock("Validating random transaction returned");
		callAndStopOnFailure(ValidateTransactionWithinRange.class);
		call(accountTransactionsValidationSequence());
		eventLog.startBlock("Add booking date query parameters");
		callAndStopOnFailure(AddBookingDate6MonthsOlderThanCurrent.class);
		preCallProtectedResource("Fetch Account transactions with query parameters");
		eventLog.startBlock("Validating random transaction returned");
		callAndStopOnFailure(ValidateTransactionWithinRange.class);
		call(accountTransactionsValidationSequence());
		eventLog.startBlock("Add booking date query parameters using value from transaction returned");
		callAndStopOnFailure(AddSavedTransactionDateAsBookingParam.class);
		preCallProtectedResource("Fetch Account transactions with query parameters");
		eventLog.startBlock("Validating random transaction returned");
		callAndStopOnFailure(ValidateTransactionWithinRange.class);
		call(accountTransactionsValidationSequence());
	}

	private ConditionSequence accountTransactionsValidationSequence(){
		return sequenceOf(
			condition(AccountTransactionsValidator.class),
			condition(EnsureResponseHasLinks.class),
			condition(ValidateResponseMetaData.class),
			sequence(ValidateSelfEndpoint.class)
		);
	}
}
