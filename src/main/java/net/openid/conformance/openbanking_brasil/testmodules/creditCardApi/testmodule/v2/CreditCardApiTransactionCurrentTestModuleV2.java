package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.testmodule.v2;


import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CardAccountsDataResponseResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.creditCard.v2.CreditCardAccountsTransactionResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@PublishTestModule(
	testName = "credit-card-api-transactions-current-test-v2",
	displayName = "Test that the server has correctly implemented the current transactions resource",
	summary = "Test that the server has correctly implemented the current transactions resource\n" +
		"\u2022 Creates a consent with only Credit Cards permissions\n" +
		"\u2022 Expect - 201 code and successful redirect\n" +
		"\u2022 Using the consent created, call the Credit Cards API\n" +
		"\u2022 Call the GET Credit Cards Accounts API V2\n" +
		"\u2022 Expect OK 200 - Validate all fields of the API - Fetch the first returned account ids to be used on the transactions API Call\n" +
		"\u2022 Call the GET Current Credit Cards Transactions API V2\n" +
		"\u2022 Expect OK 200 - Validate all fields of the API - Make sure if one transaction is found it has today’s date on it\n" +
		"\u2022 Call the GET Current Credit Cards Transactions API V2, send query parameters fromBookingDate and toBookingDate using the max 7 day period\n" +
		"\u2022 Expect OK 200 - Validate all fields of the API - Make sure if transactions are found that none of them are more than 1 week older\n" +
		"\u2022 Call the GET Current Credit Cards Transactions API V2, send query parameters fromBookingDate and toBookingDate using a period that is not over the expected valid period\n" +
		"\u2022 Expect 422 Unprocessable Entity\n",
	profile = OBBProfile.OBB_PROFIlE_PHASE2,
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
public class CreditCardApiTransactionCurrentTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildCreditCardsAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(CardAccountsDataResponseResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(CardAccountSelector.class);
		callAndStopOnFailure(PrepareUrlForFetchingCurrentAccountTransactions.class);

//		 Call without parameters
		runInBlock("Fetch Credit Card Account Current transactions V2", () -> call(getPreCallProtectedResourceSequence()));
		runInBlock("Validate Credit Card Account Current Transactions V2",
			() -> call(getValidationSequence()
				.then(condition(EnsureTransactionsDateIsSetToToday.class)))
		);

		// Call with valid  parameters
		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		env.putString("fromBookingDate", currentDate.minusDays(6).format(FORMATTER));
		env.putString("toBookingDate", currentDate.format(FORMATTER));

		callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);
		runInBlock("Fetch Credit Card Account Current transactions with valid date parameters", () -> call(getPreCallProtectedResourceSequence()));
		runInBlock("Validate Credit Card Account Current Transactions",
			() -> call(getValidationSequence()
				.then(condition(EnsureTransactionsDateIsNoOlderThan7Days.class)))
		);

		// Call with invalid  parameters
		env.putString("fromBookingDate", currentDate.minusDays(30).format(FORMATTER));
		env.putString("toBookingDate", currentDate.minusDays(20).format(FORMATTER));

		callAndStopOnFailure(PrepareUrlForFetchingCurrentAccountTransactions.class);
		callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);
		runInBlock("Fetch Credit Card Account Current transactions V2 with invalid date parameters",
			() -> call(getPreCallProtectedResourceSequence()
				.replace(EnsureResponseCodeWas200.class, condition(EnsureResponseCodeWas422.class)))
		);
		callAndStopOnFailure(ResourceErrorMetaValidator.class);

	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllCreditCardRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(AddCreditCardScopes.class);
	}

	protected ConditionSequence getValidationSequence() {
		return sequenceOf(
			condition(CreditCardAccountsTransactionResponseValidatorV2.class),
			condition(EnsureResponseHasLinks.class),
			condition(ValidateTransactionsMetaOnlyRequestDateTime.class)
		);
	}

	protected ConditionSequence getPreCallProtectedResourceSequence() {
		return sequenceOf(
			condition(CreateEmptyResourceEndpointRequestHeaders.class),
			condition(AddFAPIAuthDateToResourceEndpointRequest.class),
			condition(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class),
			condition(CreateRandomFAPIInteractionId.class),
			condition(AddFAPIInteractionIdToResourceEndpointRequest.class),
			condition(CallProtectedResource.class),
			condition(EnsureResponseCodeWas200.class),
			condition(CheckForDateHeaderInResourceResponse.class),
			condition(CheckForFAPIInteractionIdInResourceResponse.class),
			condition(EnsureResourceResponseReturnedJsonContentType.class)
		);
	}

}
