package net.openid.conformance.openbanking_brasil.testmodules.v2.accounts;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.account.v1.AccountBalancesResponseValidator;
import net.openid.conformance.openbanking_brasil.account.v2.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@PublishTestModule(
	testName = "accounts-api-operational-limits",
	displayName = "Accounts Api operational limits test module",
	summary = "The test will require a DCR to be executed prior to the test against a server whose credentials are provided here Operational Limits · Wiki · Open Banking Brasil / Certification · GitLab.\n" +
	"Make Sure that the fields “Client_id for Operational Limits Test” (client_id for OL) and at least the CPF for Operational Limits (CPF for OL) test have been provided \n" +
	"\u2022 Using the HardCoded clients provided on the test summary link, use the client_id for OL and the CPF/CNPJ for OL passed on the configuration and create a Consent Request sending the Accounts permission group - Expect Server to return a 201 - Save ConsentID (1) \n" +
	"\u2022 Redirect User to authorize the Created Consent - Expect a successful authorization\n" +
	"\u2022 With the authorized consent id (1), call the GET Accounts List API Once - Expect a 200 - Save the first returned ACTIVE resource id (R_1) and the second saved returned active resource id (R_2)\n" +
	"\u2022 With the authorized consent id (1), call the GET Accounts API with the saved Resource ID (R_1) once  - Expect a 200 response\n" +
	"\u2022 With the authorized consent id (1), call the GET Accounts Transactions API with the saved Resource ID (R_1) once, send query parameters fromBookingDate as D-6 and toBookingDate as Today - Expect a 200 response - Make Sure That at least 10 Transactions have been returned \n" +
	"\u2022 With the authorized consent id (1), call the GET Accounts Balances API with the saved Resource ID (R_1) 14 Times  - Expect a 200 response\n" +
	"\u2022 With the authorized consent id (1), call the GET Accounts Limits API with the saved Resource ID (R_1) 14 Times  - Expect a 200 response\n" +
	"\u2022 With the authorized consent id (1), call the GET Accounts Transactions-Current API with the saved Resource ID (R_1) 7 Times  - Expect a 200 response\n" +
	"\u2022 With the authorized consent id (1), call the GET Accounts Transactions-Current API with the saved Resource ID (R_1) Once,  send query parameters fromBookingDate as D-6 and toBookingDate as Today, with page-size=1 - Expect a 200 response - Fetch the links.next URI \n" +
	"\u2022 Call the GET Accounts Transactions-Current API 9 more times, always using the links.next returned and always forcing the page-size to be equal to 1 - Expect a 200 on every single call \n" +
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
		"resource.brazilCpfOperational",
		"resource.brazilCnpjOperational",
		"consent.productType"
	}
)
public class AccountsApiOperationalLimitsTestModule extends AbstractOBBrasilFunctionalTestModule {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	private int numberOfExecutions = 1;

	private static final String API_RESOURCE_ID = "accountId";
	private int numberOfIdsToFetch = 2;
	@Override
	protected void configureClient(){
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl){
		callAndStopOnFailure(AddAccountScope.class);
		callAndStopOnFailure(EnsureClientIdForOperationalLimitsIsPresent.class);
		callAndStopOnFailure(SwitchToOperationalLimitsClientId.class);
		callAndContinueOnFailure(OperationalLimitsToConsentRequest.class);
		super.onConfigure(config, baseUrl);
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		expose("consent_id " + numberOfExecutions, env.getString("consent_id"));

		if(numberOfExecutions == 1){
			callAndStopOnFailure(SwitchToOriginalClientId.class);
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
		if (numberOfExecutions == 1){
			String accountIdOne = OIDFJSON.getString(env.getObject("fetched_api_ids").getAsJsonArray("fetchedApiIds").get(0));
			env.putString("accountId", accountIdOne);
			accountsOperationalLimitCalls();
			String accountIdTwo = OIDFJSON.getString(env.getObject("fetched_api_ids").getAsJsonArray("fetchedApiIds").get(1));
			env.putString("accountId", accountIdTwo);
			accountsOperationalLimitCalls();
		} else if (numberOfExecutions == 2){
			callAndStopOnFailure(AccountSelector.class);
			callAndStopOnFailure(AccountsOperationLimitsSelectResourceOne.class);
			accountsOperationalLimitCalls();
		}


	}

	private void accountsOperationalLimitCalls(){
		preCallProtectedResource("Fetching First Account");
		//Call Account resource
		callAndStopOnFailure(PrepareUrlForFetchingAccountResource.class);

		preCallProtectedResource("Fetching First Account");
		runInBlock("Validate Account Response", () -> {
			callAndContinueOnFailure(AccountIdentificationResponseValidatorV2.class, Condition.ConditionResult.FAILURE);
		});
		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactions.class);

		LocalDate currentDate = LocalDate.now(ZoneId.of("America/Sao_Paulo"));
		env.putString("fromBookingDate", currentDate.minusDays(6).format(FORMATTER));
		env.putString("toBookingDate", currentDate.format(FORMATTER));

		callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);

		preCallProtectedResource("Fetching transactions with booking date parameters");
		runInBlock("Validate Account Transactions Response", () -> {
			callAndStopOnFailure(AccountTransactionsValidatorV2.class, Condition.ConditionResult.FAILURE);
		});

		callAndStopOnFailure(PrepareUrlForFetchingAccountBalances.class);
		for (int i = 0; i < 14; i++){
			preCallProtectedResource("Fetching balances");

			runInBlock("Validate Account Transactions Balances", () -> {
				callAndStopOnFailure(AccountBalancesResponseValidator.class, Condition.ConditionResult.FAILURE);
			});
		}

		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactionLimit.class);

		for (int i = 0; i < 14; i++){
			preCallProtectedResource("Fetching accounts limits");

			runInBlock("Validate accounts limits Balances", () -> {
				callAndStopOnFailure(AccountLimitsValidatorV2.class, Condition.ConditionResult.FAILURE);
			});
		}
		callAndStopOnFailure(PrepareUrlForFetchingAccountTransactionsCurrent.class);

		for (int j = 0; j < 7; j++){
			preCallProtectedResource("Fetching Account Transactions current");

			runInBlock("Validate Account Transactions current", () -> {
				callAndContinueOnFailure(AccountTransactionsCurrentValidatorV2.class, Condition.ConditionResult.FAILURE);
			});
		}

		callAndStopOnFailure(AddToAndFromBookingDateMaxLimitedParametersToProtectedResourceUrl.class);

		preCallProtectedResource("Fetching Accounts Transactions Current with Booking date parameters");

		runInBlock("Validate Account Transactions Balances", () -> {
			callAndContinueOnFailure(AccountTransactionsCurrentValidatorV2.class, Condition.ConditionResult.FAILURE);
		});


		callAndStopOnFailure(SetSpecifiedUrlParameterToSpecifiedValue.class);
		env.putString("protected_resource_url", env.getString("extracted_link"));

		for (int k = 0; k < 10; k++){
			preCallProtectedResource("Fetching Accounts Transactions Current next link");

			runInBlock("Validate Account Transactions Balances", () -> {
				callAndContinueOnFailure(AccountTransactionsCurrentValidatorV2.class, Condition.ConditionResult.FAILURE);
				env.putString("value", "1");
				env.putString("parameter", "page-size");
				callAndStopOnFailure(SetSpecifiedUrlParameterToSpecifiedValue.class);
				env.putString("protected_resource_url", env.getString("extracted_link"));
			});
		}

	}
}
