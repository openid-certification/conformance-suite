package net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.testmodule.v2;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditCard.v2.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@PublishTestModule(
	testName = "credit-cards-api-operational-limits",
	displayName = "This test will make sure that the server is not blocking access to the APIs as long as the operational limits for the Credit Cards API are considered correctly and, if present, that the pagination-key parameter is correctly serving it’s function\n",
	summary = "The test will require a DCR to be executed prior to the test against a server whose credentials are provided here https://gitlab.com/obb1/certification/-/wikis/Operational-Limits\n" +
		"This test will require the user to have set at least two ACTIVE resources each with at least 10 Transactions to be returned on the transactions-current endpoint for each active account\n" +
		"This test will make sure that the server is not blocking access to the APIs as long as the operational limits for the Credit Cards API are considered correctly and, if present, that the pagination-key parameter is correctly serving it’s function\n" +
		"\u2022 Make Sure that the fields “Client_id for Operational Limits Test” (client_id for OL) and at least the CPF for Operational Limits (CPF for OL) test have been provided\n" +
		"\u2022 Using the HardCoded clients provided on the test summary link, use the client_id for OL and the CPF/CNPJ for OL passed on the configuration and create a Consent Request sending the Credit Cards permission group - Expect Server to return a 201 - Save ConsentID (1)\n" +
		"\u2022 Redirect User to authorize the Created Consent - Expect a successful authorization\n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards List API Once - Expect a 200 - Save the first returned ACTIVE resource id (R_1) and the second saved returned active resource id (R_2)\n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards API with the saved Resource ID (R_1) once  - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards Transactions API with the saved Resource ID (R_1) once, send query parameters fromBookingDate as D-6 and toBookingDate as Today - Expect a 200 response - Make Sure That at least 10 Transactions have been returned \n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards Bills API with the saved Resource ID (R_1) Once  - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards Limits API with the saved Resource ID (R_1) Once - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards Transactions-Current API with the saved Resource ID (R_1) 7 Times  - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards Transactions-Current API with the saved Resource ID (R_1) Once, send query parameters fromBookingDate as D-6 and toBookingDate as Today, with page-size=1 - Expect a 200 response - Fetch the links.next URI\n" +
		"\u2022 Call the GET Credit Cards Transactions-Current API 9 more times, always using the links.next returned and always forcing the page-size to be equal to 1 - Expect a 200 on every single call\n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards API with the saved Resource ID (R_2) once - Expect a 200 response\n" +
		"\u2022 Repeat the exact same process done with the first tested resources (R_1) but now, execute it against the second returned Resource (R_2) \n" +
		"\u2022 Using the regular client_id provided and the regular CPF/CNPJ for OL create a Consent Request sending the Credit Cards permission group - Expect Server to return a 201 - Save ConsentID (2)\n" +
		"\u2022 Redirect User to authorize the Created Consent - Expect a successful authorization\n" +
		"\u2022 With the authorized consent id (2), call the GET Credit Cards List API Once - Expect a 200 - Save the first returned ACTIVE resource id (R_1) \n" +
		"\u2022 With the authorized consent id (2), call the GET Credit Cards API with the saved Resource ID (R_1) once - Expect a 200 response\n" +
		"\u2022 Repeat the exact same process done with the first tested resources (R_1) done with the first client",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.client_id_operational_limits",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"resource.brazilCnpj",
		"resource.brazilCpfOperational",
		"resource.brazilCnpjOperational",
		"consent.productType"
	}
)
public class CreditCardsApiOperationalLimitsTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private static final String API_RESOURCE_ID = "creditCardAccountId";
	private int numberOfIdsToFetch = 2;

	private static final int REQUIRED_NUMBER_OF_RECORDS = 10;

	private int numberOfExecutions = 1;


	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildCreditCardsAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddCreditCardScopes.class);
		callAndStopOnFailure(PrepareAllCreditCardRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(EnsureClientIdForOperationalLimitsIsPresent.class);
		callAndStopOnFailure(SwitchToOperationalLimitsClientId.class);
		callAndContinueOnFailure(OperationalLimitsToConsentRequest.class);
		super.onConfigure(config, baseUrl);
	}

	@Override
	protected void validateResponse() {

		call(getValidationSequence(CardAccountsDataResponseResponseValidatorV2.class));
		eventLog.endBlock();

		runInBlock("Preparing Credit Card Accounts", () -> {
			env.putString("apiIdName", API_RESOURCE_ID);
			callAndStopOnFailure(ExtractAllSpecifiedApiIds.class);

			env.putInteger("number_of_ids_to_fetch", numberOfIdsToFetch);
			callAndStopOnFailure(FetchSpecifiedNumberOfExtractedApiIds.class);
		});

		for (int i = 0; i < numberOfIdsToFetch; i++) {

			// Call to credit card account GET

			String creditCardAccountId = OIDFJSON.getString(env.getObject("fetched_api_ids").getAsJsonArray("fetchedApiIds").get(i));
			env.putString("accountId", creditCardAccountId);
			callAndStopOnFailure(PrepareUrlForFetchingCreditCardAccount.class);

			preCallProtectedResource(String.format("Fetching Credit Card Account using resource_id_%d and consent_id_%d", i + 1, numberOfExecutions));
			runInBlock("Validate Credit Card Account response",
				() -> getValidationSequence(CardIdentificationResponseValidatorV2.class));


			// Call to credit card transactions  with dates GET

			LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
			env.putString("fromBookingDate", currentDate.minusDays(6).format(FORMATTER));
			env.putString("toBookingDate", currentDate.format(FORMATTER));

			callAndStopOnFailure(PrepareUrlForFetchingCardTransactions.class);
			callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);

			preCallProtectedResource(String.format("Fetch Credit Card Transactions using resource_id_%d and consent_id_%d", i + 1, numberOfExecutions));

			runInBlock("Validate Credit Card Transactions Response", () -> {
				getValidationSequence(CreditCardAccountsTransactionResponseValidatorV2.class);
				env.putInteger("required_number_of_records", REQUIRED_NUMBER_OF_RECORDS);
				callAndStopOnFailure(EnsureAtLeastSpecifiedNumberOfRecordsWereReturned.class);
			});



			// Call to credit card bills GET

			callAndStopOnFailure(PrepareUrlForFetchingCardBills.class);
			preCallProtectedResource(String.format("Fetch Credit Card Bills using resource_id_%d and and consent_id_%d", i + 1, numberOfExecutions));
			runInBlock("Validate Credit Card Bills Response", () -> getValidationSequence(CreditCardBillValidatorV2.class));



			// Call to credit card limits GET

			callAndStopOnFailure(PrepareUrlForFetchingCardLimits.class);
			preCallProtectedResource(String.format("Fetch Credit Card Limits using resource_id_%d and consent_id_%d", i + 1, numberOfExecutions));
			runInBlock("Validate Credit Card Limits Response",
				() -> getValidationSequence(CreditCardAccountsLimitsResponseValidatorV2.class));



			// Call to credit card current transactions 7 times GET

			callAndStopOnFailure(PrepareUrlForFetchingCurrentAccountTransactions.class);
			for (int j = 0; j < REQUIRED_NUMBER_OF_RECORDS - 3; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Credit Card Transactions Current using resource_id_%d and consent_id_%d", j + 1, i + 1, numberOfExecutions));
				runInBlock("Validate Credit Card Transactions Current Response",
					() -> getValidationSequence(CreditCardAccountsTransactionCurrentResponseValidatorV2.class));
			}

			callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);
			env.putInteger("required_page_size", 1);
			callAndStopOnFailure(AddSpecifiedPageSizeParameterToProtectedResourceUrl.class);



			// Call to credit card current transactions with dates and page size fetched from next link 10 times GET

			for (int j = 0; j < REQUIRED_NUMBER_OF_RECORDS; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Credit Card Transactions Current next link using resource_id_%d and consent_id_%d", j + 1, i + 1, numberOfExecutions));

				runInBlock("Validate Credit Card Transactions Current Response",
					() -> {
						getValidationSequence(CreditCardAccountsTransactionCurrentResponseValidatorV2.class);
						callAndStopOnFailure(ValidateNumberOfRecordsPage1.class);
						callAndStopOnFailure(EnsureOnlyOneRecordWasReturned.class);
						callAndStopOnFailure(ExtractNextLink.class);

						env.putString("value", "1");
						env.putString("parameter", "page-size");
						callAndStopOnFailure(SetSpecifiedUrlParameterToSpecifiedValue.class);
						env.putString("protected_resource_url", env.getString("extracted_link"));
					});
			}


		}

	}


	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (numberOfExecutions == 1) {
			callAndStopOnFailure(PrepareUrlForCreditCardRoot.class);
			callAndStopOnFailure(SwitchToOriginalClientId.class);
			callAndStopOnFailure(RemoveOperationalLimitsFromConsentRequest.class);
			callAndStopOnFailure(RemoveConsentIdFromClientScopes.class);
			validationStarted = false;
			numberOfExecutions++;
			numberOfIdsToFetch = 1;

			performAuthorizationFlow();
		} else {
			fireTestFinished();
		}
	}

	protected ConditionSequence getValidationSequence(Class<? extends Condition> validationClass) {
		return sequenceOf(
			condition(EnsureResponseCodeWas200.class),
			condition(validationClass),
			condition(ValidateResponseMetaData.class)
		);
	}
}
