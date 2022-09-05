package net.openid.conformance.openbanking_brasil.testmodules.v2.operationalLimits;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditCard.v2.*;
import net.openid.conformance.openbanking_brasil.testmodules.account.PrepareUrlForFetchingCurrentAccountTransactions;
import net.openid.conformance.openbanking_brasil.testmodules.creditCardApi.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

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
		"\u2022 Return a Success if Consent Response is a 201 containing all permissions required on the scope of the test. Return a Warning and end the test if the consent request returns either a 422 or a 201 without Permission for this specific test.\n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards List API Once - Expect a 200 - Save the first returned ACTIVE resource id (R_1) and the second saved returned active resource id (R_2)\n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards API with the saved Resource ID (R_1) 30 Times  - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards Transactions API with the saved Resource ID (R_1) 30 Times, send query parameters fromBookingDate as D-6 and toBookingDate as Today - Expect a 200 response - On the first API Call Make Sure That at least 20 Transactions have been returned \n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards Bills API with the saved Resource ID (R_1) 30 Times  - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards Limits API with the saved Resource ID (R_1) 30 Times - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards Transactions-Current API with the saved Resource ID (R_1) 230 Times  - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (1), call the GET Credit Cards Transactions-Current API with the saved Resource ID (R_1) Once, send query parameters fromBookingDate as D-6 and toBookingDate as Today, with page-size=1 - Expect a 200 response - Fetch the links.next URI\n" +
		"\u2022 Call the GET Credit Cards Transactions-Current API 19 more times, always using the links.next returned and always forcing the page-size to be equal to 1 - Expect a 200 on every single call\n" +
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
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.consentUrl",
		"resource.brazilCpfPersonal",
		"resource.brazilCpfBusiness",
		"resource.brazilCnpjBusiness",
		"resource.brazilCpfOperationalPersonal",
		"resource.brazilCpfOperationalBusiness",
		"resource.brazilCnpjOperationalBusiness",
		"consent.productType"
	}
)
public class CreditCardsApiOperationalLimitsTestModuleV2 extends AbstractOperationalLimitsTestModule {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private static final String API_RESOURCE_ID = "creditCardAccountId";
	private int numberOfIdsToFetch = 2;

	private static final int REQUIRED_NUMBER_OF_RECORDS = 20;

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
		switchToSecondClient();
		callAndStopOnFailure(AddCreditCardScopes.class);
		callAndContinueOnFailure(OperationalLimitsToConsentRequest.class);
		clientAuthType = getVariant(ClientAuthType.class);
		super.onConfigure(config, baseUrl);
	}


	@Override
	protected void validateResponse() {
		// Validate credit card response
		call(getValidationSequence(CardAccountsDataResponseResponseValidatorV2.class, ValidateResponseMetaData.class));
		eventLog.endBlock();

		runInBlock("Preparing Credit Card Accounts", () -> {
			env.putString("apiIdName", API_RESOURCE_ID);
			callAndStopOnFailure(ExtractAllSpecifiedApiIds.class);

			env.putInteger("number_of_ids_to_fetch", numberOfIdsToFetch);
			callAndStopOnFailure(FetchSpecifiedNumberOfExtractedApiIds.class);
		});
		for (int i = 0; i < numberOfIdsToFetch; i++) {
			int currentResourceId = i + 1;

			// Call to credit card account GET once with validation
			String creditCardAccountId = OIDFJSON.getString(env.getObject("fetched_api_ids").getAsJsonArray("fetchedApiIds").get(i));
			runInLoggingBlock(() -> {
				env.putString("accountId", creditCardAccountId);
				callAndStopOnFailure(PrepareUrlForFetchingCreditCardAccount.class);

				preCallProtectedResource(String.format("Fetching Credit Card Account using resource_id_%d and consent_id_%d", currentResourceId, numberOfExecutions));
				validateResponse("Validate Credit Card Account response", CardIdentificationResponseValidatorV2.class);
			});


			// Call to credit card account GET 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetching Credit Card Account using resource_id_%d and consent_id_%d", j + 1, currentResourceId, numberOfExecutions));
			}

			// Call to credit card transactions  with dates GET once with validation
			LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

			runInLoggingBlock(() -> {
				env.putString("fromTransactionDate", currentDate.minusDays(6).format(FORMATTER));
				env.putString("toTransactionDate", currentDate.format(FORMATTER));

				callAndStopOnFailure(PrepareUrlForFetchingCardTransactions.class);
				callAndStopOnFailure(AddToAndFromTransactionDateParametersToProtectedResourceUrl.class);


				env.putInteger("required_number_of_records", REQUIRED_NUMBER_OF_RECORDS);

				preCallProtectedResource(String.format("Fetch Credit Card Transactions using resource_id_%d and consent_id_%d", currentResourceId, numberOfExecutions));
				validateResponse("Validate Credit Card Transactions Response", CreditCardAccountsTransactionResponseValidatorV2.class, ValidateMetaOnlyRequestDateTime.class);
				callAndStopOnFailure(EnsureAtLeastSpecifiedNumberOfRecordsWereReturned.class);
			});


			// Call to credit card transactions  with dates GET 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Credit Card Transactions using resource_id_%d and consent_id_%d", j + 1, currentResourceId, numberOfExecutions));
			}

			// Call to credit card bills GET Once with Validation
			runInLoggingBlock(() -> {
				env.putString("fromDueDate", currentDate.minusDays(6).format(FORMATTER));
				env.putString("toDueDate", currentDate.format(FORMATTER));
				callAndStopOnFailure(AddToAndFromDueDateParametersToProtectedResourceUrl.class);
				callAndStopOnFailure(PrepareUrlForFetchingCardBills.class);
				preCallProtectedResource(String.format("Fetch Credit Card Bills using resource_id_%d and and consent_id_%d", currentResourceId, numberOfExecutions));
				validateResponse("Validate Credit Card Bills Response", CreditCardBillValidatorV2.class);
			});

			// Call to credit card bills GET 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Credit Card Bills using resource_id_%d and and consent_id_%d", j + 1, currentResourceId, numberOfExecutions));
			}

			refreshAccessToken();


			// Call to credit card limits GET once with validation

			runInLoggingBlock(() -> {
				callAndStopOnFailure(PrepareUrlForFetchingCardLimits.class);

				preCallProtectedResource(String.format("Fetch Credit Card Limits using resource_id_%d and consent_id_%d", currentResourceId, numberOfExecutions));
				validateResponse("Validate Credit Card Limits Response", CreditCardAccountsLimitsResponseValidatorV2.class);

			});

			// Call to credit card limits GET 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Credit Card Limits using resource_id_%d and consent_id_%d", j + 1, currentResourceId, numberOfExecutions));
			}

			// Call to credit card current transactions GET once with validation
			runInLoggingBlock(() -> {
				callAndStopOnFailure(PrepareUrlForFetchingCurrentAccountTransactions.class);

				preCallProtectedResource(String.format("Fetch Credit Card Transactions Current using resource_id_%d and consent_id_%d", currentResourceId, numberOfExecutions));
				validateResponse("Validate Credit Card Transactions Current Response", CreditCardAccountsTransactionCurrentResponseValidatorV2.class, ValidateMetaOnlyRequestDateTime.class);

			});
			// Call to credit card current transactions GET 229 times refreshing token every 100 calls
			for (int j = 1; j < 230; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Credit Card Transactions Current using resource_id_%d and consent_id_%d", j + 1, currentResourceId, numberOfExecutions));

				if (j % 100 == 0) {
					refreshAccessToken();
				}
			}


			// Call to credit card current transactions with dates and page size fetched from next once with validation
			runInLoggingBlock(() -> {
				env.putString("fromTransactionDateMaxLimited", currentDate.minusDays(6).format(FORMATTER));
				env.putString("toTransactionDateMaxLimited", currentDate.format(FORMATTER));

				callAndStopOnFailure(AddToAndFromTransactionDateMaxLimitedParametersToProtectedResourceUrl.class);
				env.putInteger("required_page_size", 1);
				callAndStopOnFailure(AddSpecifiedPageSizeParameterToProtectedResourceUrl.class);

				preCallProtectedResource(String.format("Fetch Credit Card Transactions Current next link using resource_id_%d and consent_id_%d", currentResourceId, numberOfExecutions));
				validateResponse("Validate Credit Card Transactions Current Response", CreditCardAccountsTransactionCurrentResponseValidatorV2.class, ValidateMetaOnlyRequestDateTime.class);
				validateNextLinkResponse();
			});

			// Call to credit card current transactions with dates and page size fetched from next link 19 times GET
			for (int j = 1; j < REQUIRED_NUMBER_OF_RECORDS; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Credit Card Transactions Current next link using resource_id_%d and consent_id_%d", j + 1, currentResourceId, numberOfExecutions));
				validateNextLinkResponse();
			}
			enableLogging();
		}

	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		enableLogging();
		if (numberOfExecutions == 1) {
			callAndStopOnFailure(PrepareUrlForCreditCardRoot.class);
			unmapClient();
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


	private void validateNextLinkResponse() {
		runInBlock("Validate Credit Card Transactions Current Response Records and fetch next link",
			() -> {
				callAndStopOnFailure(ValidateNumberOfRecordsPage1.class);
				callAndStopOnFailure(EnsureOnlyOneRecordWasReturned.class);
				callAndStopOnFailure(ExtractNextLink.class);

				env.putString("value", "1");
				env.putString("parameter", "page-size");
				callAndStopOnFailure(SetSpecifiedValueToSpecifiedUrlParameter.class);
				env.putString("protected_resource_url", env.getString("extracted_link"));
			});
	}

	protected void validateResponse(String message, Class<? extends Condition> validationClass) {
		runInBlock(message, () -> call(getValidationSequence(validationClass, ValidateResponseMetaData.class)));
	}

	protected void validateResponse(String message, Class<? extends Condition> validationClass,
									Class<? extends Condition> metaValidatorClass) {
		runInBlock(message, () -> call(getValidationSequence(validationClass, metaValidatorClass)));
	}

	protected ConditionSequence getValidationSequence(Class<? extends Condition> validationClass,
													  Class<? extends Condition> metaValidatorClass) {
		return sequenceOf(
			condition(validationClass),
			condition(metaValidatorClass)
		);
	}

}
