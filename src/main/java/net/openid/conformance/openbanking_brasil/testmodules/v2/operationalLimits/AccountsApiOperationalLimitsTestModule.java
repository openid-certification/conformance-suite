package net.openid.conformance.openbanking_brasil.testmodules.v2.operationalLimits;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.account.v2.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.account.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.GenerateRefreshTokenRequest;
import net.openid.conformance.openbanking_brasil.testmodules.v2.GenerateRefreshAccessTokenSteps;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@PublishTestModule(
	testName = "accounts-api-operational-limits",
	displayName = "Accounts Api operational limits test module",
	summary = "The test will require a DCR to be executed prior to the test against a server whose credentials are provided here https://gitlab.com/obb1/certification/-/wikis/Operational-Limits\n" +
		"Make Sure that the fields “Client_id for Operational Limits Test” (client_id for OL) and at least the CPF for Operational Limits (CPF for OL) test have been provided \n" +
		"\u2022 Using the HardCoded clients provided on the test summary link, use the client_id for OL and the CPF/CNPJ for OL passed on the configuration and create a Consent Request sending the Accounts permission group - Expect Server to return a 201 - Save ConsentID (1) \n" +
		"\u2022 Return a Success if Consent Response is a 201 containing all permissions required on the scope of the test. Return a Warning and end the test if the consent request returns either a 422 or a 201 without Permission for this specific test.\n" +
		"\u2022 With the authorized consent id (1), call the GET Accounts List API Once - Expect a 200 - Save the first returned ACTIVE resource id (R_1) and the second saved returned active resource id (R_2)\n" +
		"\u2022 With the authorized consent id (1), call the GET Accounts API with the saved Resource ID (R_1) 30 Times - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (1), call the GET Accounts Transactions API with the saved Resource ID (R_1) 30 Times, send query parameters fromBookingDate as D-6 and toBookingDate as Today - Expect a 200 response - Make Sure That at least 20 Transactions have been returned \n" +
		"\u2022 With the authorized consent id (1), refresh Access Token using the Refresh Token" +
		"\u2022 With the authorized consent id (1), call the GET Accounts Balances API with the saved Resource ID (R_1) 420 Times  - Expect a 200 response. Every 100 calls the Access token is refreshed.\n" +
		"\u2022 With the authorized consent id (1), call the GET Accounts Limits API with the saved Resource ID (R_1) 420 Times  - Expect a 200 response. Every 100 calls the Access token is refreshed.\n" +
		"\u2022 With the authorized consent id (1), call the GET Accounts Transactions-Current API with the saved Resource ID (R_1) 210 Times  - Expect a 200 response. Every 100 calls the Access token is refreshed.\n" +
		"\u2022 With the authorized consent id (1), refresh Access Token using the Refresh Token" +
		"\u2022 With the authorized consent id (1), call the GET Accounts Transactions-Current API with the saved Resource ID (R_1) Once,  send query parameters fromBookingDate as D-6 and toBookingDate as Today, with page-size=1 - Expect a 200 response - Fetch the links.next URI \n" +
		"\u2022 Call the GET Accounts Transactions-Current API 19 more times, always using the links.next returned and always forcing the page-size to be equal to 1 - Expect a 200 on every single call \n" +
		"\u2022 With the authorized consent id (1), call the GET Accounts API with the saved Resource ID (R_2) once - Expect a 200 response \n" +
		"\u2022 Repeat the exact same process done with the first tested resources (R_1) but now, execute it against the second returned Resource (R_2) \n" +
		"\u2022 Using the regular client_id provided and the regular CPF/CNPJ for OL create a Consent Request sending the Accounts permission group - Expect Server to return a 201 - Save ConsentID (2) \n" +
		"\u2022 Redirect User to authorize the Created Consent - Expect a successful authorization\n" +
		"\u2022 With the authorized consent id (2), call the GET Accounts List API Once - Expect a 200 - Save the first returned ACTIVE resource id (R_1)\n" +
		"\u2022 With the authorized consent id (2), call the GET Accounts API with the saved Resource ID (R_1) once - Expect a 200 response\n" +
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
		"resource.brazilCpfOperationalPersonal",
		"resource.brazilCpfOperationalBusiness",
		"resource.brazilCnpjOperationalBusiness",
		"consent.productType"
	}
)
public class AccountsApiOperationalLimitsTestModule extends AbstractOBBrasilFunctionalTestModule {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final int REQUIRED_NUMBER_OF_RECORDS = 20;
	private static final String API_RESOURCE_ID = "accountId";

	private int numberOfExecutions = 1;
	private int numberOfIdsToFetch = 2;
	private ClientAuthType clientAuthType;

	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddAccountScope.class);
		callAndStopOnFailure(PrepareAllAccountRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(EnsureClientIdForOperationalLimitsIsPresent.class);
		callAndStopOnFailure(SwitchToOperationalLimitsClient.class);
		callAndContinueOnFailure(OperationalLimitsToConsentRequest.class);
		clientAuthType = getVariant(ClientAuthType.class);
		super.onConfigure(config, baseUrl);
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		return new OpenBankingBrazilPreAuthorizationSteps(isSecondClient(), false, addTokenEndpointClientAuthentication, brazilPayments.isTrue(), true);
	}

	@Override
	protected void performPreAuthorizationSteps() {
		super.performPreAuthorizationSteps();

		call(exec().mapKey("endpoint_response", "consent_endpoint_response_full"));
		callAndContinueOnFailure(EnsureHttpStatusCodeIs201.class, Condition.ConditionResult.WARNING);

		if (getResult() == Result.WARNING) {
			fireTestFinished();
		}else {
			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE);
			call(exec().unmapKey("endpoint_response"));
			callAndContinueOnFailure(FAPIBrazilConsentEndpointResponseValidatePermissions.class, Condition.ConditionResult.WARNING);

			if (getResult() == Result.WARNING) {
				fireTestFinished();
			}else {
				callAndContinueOnFailure(EnsureResponseHasLinksForConsents.class, Condition.ConditionResult.FAILURE);
				callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
				callAndStopOnFailure(ExtractConsentIdFromConsentEndpointResponse.class);
				callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11", "FAPI1-BASE-6.2.1-11");
				callAndStopOnFailure(FAPIBrazilAddConsentIdToClientScope.class);
			}
		}
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		expose("consent_id " + numberOfExecutions, env.getString("consent_id"));

		if (numberOfExecutions == 1) {
			callAndStopOnFailure(SwitchToOriginalClient.class);
			callAndStopOnFailure(RemoveOperationalLimitsFromConsentRequest.class);
			callAndContinueOnFailure(RemoveConsentIdFromClientScopes.class);
			callAndStopOnFailure(PrepareUrlForFetchingAccounts.class);
			validationStarted = false;
			numberOfExecutions++;
			performAuthorizationFlow();
		} else {
			fireTestFinished();
		}
	}

	@Override
	protected void validateResponse() {
		callAndStopOnFailure(AccountListValidatorV2.class);
		runInBlock("Preparing Credit Card Accounts", () -> {
			env.putString("apiIdName", API_RESOURCE_ID);
			callAndStopOnFailure(ExtractAllSpecifiedApiIds.class);

			env.putInteger("number_of_ids_to_fetch", numberOfIdsToFetch);
			callAndStopOnFailure(FetchSpecifiedNumberOfExtractedApiIds.class);
		});
		if (numberOfExecutions == 1) {
			String accountIdOne = OIDFJSON.getString(env.getObject("fetched_api_ids").getAsJsonArray("fetchedApiIds").get(0));
			env.putString("accountId", accountIdOne);
			accountsOperationalLimitCalls();
			String accountIdTwo = OIDFJSON.getString(env.getObject("fetched_api_ids").getAsJsonArray("fetchedApiIds").get(1));
			env.putString("accountId", accountIdTwo);
			accountsOperationalLimitCalls();
			numberOfIdsToFetch--;
		} else if (numberOfExecutions == 2) {
			String accountIdOne = OIDFJSON.getString(env.getObject("fetched_api_ids").getAsJsonArray("fetchedApiIds").get(0));
			env.putString("accountId", accountIdOne);
			accountsOperationalLimitCalls();
		}


	}

	private void accountsOperationalLimitCalls() {
		//Call Account resource
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);

		for (int i = 0; i < 30; i++) {
			preCallProtectedResource(String.format("[%d] Fetching First Account", i + 1));
			if (i == 0) {
				runInBlock("Validate Account Response", () -> {
					callAndContinueOnFailure(AccountIdentificationResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
					callAndStopOnFailure(ValidateResponseMetaData.class);
				});
			}
		}
		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);

		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		env.putString("fromBookingDate", currentDate.minusDays(6).format(FORMATTER));
		env.putString("toBookingDate", currentDate.format(FORMATTER));

		callAndStopOnFailure(AddToAndFromBookingDateParametersToProtectedResourceUrl.class);

		env.putInteger("required_number_of_records", REQUIRED_NUMBER_OF_RECORDS);
		for (int i = 0; i < 30; i++) {
			preCallProtectedResource(String.format("[%d] Fetching transactions with booking date parameters", i + 1));

			if (i == 0) {
				runInBlock("Validate Account Transactions Response", () -> {
					callAndContinueOnFailure(AccountTransactionsValidatorV2.class, Condition.ConditionResult.FAILURE);
					callAndContinueOnFailure(ValidateMetaOnlyRequestDateTime.class);
					callAndStopOnFailure(EnsureAtLeastSpecifiedNumberOfRecordsWereReturned.class);
				});
			}
		}

		callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);

		for (int i = 0; i < 420; i++) {
			preCallProtectedResource(String.format("[%d] Fetching balances", i + 1));
			validateFields(i, "Validate Account Transactions Balances", AccountBalancesResponseValidatorV2.class);
		}

		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactionLimit.class);

		for (int i = 0; i < 420; i++) {
			preCallProtectedResource(String.format("[%d] Fetching accounts limits", i + 1));
			validateFields(i, "Validate accounts limits Balances", AccountLimitsValidatorV2.class);
		}

		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactionsCurrent.class);

		for (int i = 0; i < 210; i++) {
			preCallProtectedResource(String.format("[%d] Fetching Account Transactions current", i + 1));
			validateFields(i, "Validate Account Transactions current", AccountTransactionsCurrentValidatorV2.class);
		}

		env.putString("fromBookingDateMaxLimited", currentDate.minusDays(6).format(FORMATTER));
		env.putString("toBookingDateMaxLimited", currentDate.format(FORMATTER));

		callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);
		env.putInteger("required_page_size", 1);
		callAndContinueOnFailure(AddSpecifiedPageSizeParameterToProtectedResourceUrl.class);

		refreshAccessToken();

		for (int i = 0; i < REQUIRED_NUMBER_OF_RECORDS; i++) {
			preCallProtectedResource(String.format("[%d] Fetching Accounts Transactions Current next link", i + 1));


			eventLog.startBlock(String.format("[%d] Validate Account Transactions Balances", i + 1));

			if (i == 0) {
				callAndContinueOnFailure(AccountTransactionsCurrentValidatorV2.class, Condition.ConditionResult.FAILURE);
			}
			callAndStopOnFailure(ValidateNumberOfRecordsPage1.class);
			callAndStopOnFailure(EnsureOnlyOneRecordWasReturned.class);
			callAndStopOnFailure(ExtractNextLink.class);

			env.putString("value", "1");
			env.putString("parameter", "page-size");
			callAndStopOnFailure(SetSpecifiedValueToSpecifiedUrlParameter.class);
			env.putString("protected_resource_url", env.getString("extracted_link"));

			eventLog.endBlock();
		}

	}

	private void validateFields(int i, String message, Class<? extends Condition> conditionClass) {
		if (i == 0) {
			runInBlock(message, () -> callAndStopOnFailure(conditionClass, Condition.ConditionResult.FAILURE));
		}
		if (i % 100 == 0) {
			refreshAccessToken();
		}
	}

	private void refreshAccessToken() {
		GenerateRefreshAccessTokenSteps refreshAccessTokenSteps = new GenerateRefreshAccessTokenSteps(clientAuthType);
		call(refreshAccessTokenSteps);
	}
}
