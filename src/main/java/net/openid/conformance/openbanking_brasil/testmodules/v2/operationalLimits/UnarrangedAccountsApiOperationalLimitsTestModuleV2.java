package net.openid.conformance.openbanking_brasil.testmodules.v2.operationalLimits;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.advances.v2.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.advances.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.v2.GenerateRefreshAccessTokenSteps;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;


@PublishTestModule(
	testName = "unarranged-overdraft-api-operational-limits",
	displayName = "This test will make sure that the server is not blocking access to the APIs as long as the operational limits for the Unarranged Accounts API are considered correctly and, if present, that the pagination-key parameter is correctly serving it’s function\n",
	summary = "The test will require a DCR to be executed prior to the test against a server whose credentials are provided here https://gitlab.com/obb1/certification/-/wikis/Operational-Limits .\n" +
		"This test will require the user to have set at least two ACTIVE resources on the Unarranged Accounts API. \n" +
		"This test will make sure that the server is not blocking access to the APIs as long as the operational limits for the Unarranged Accounts API are considered correctly.\n" +
		"\u2022 Make Sure that the fields “Client_id for Operational Limits Test” (client_id for OL) and at least the CPF for Operational Limits (CPF for OL) test have been provided\n" +
		"\u2022 Using the HardCoded clients provided on the test summary link, use the client_id for OL and the CPF/CNPJ for OL passed on the configuration and create a Consent Request sending the Credit Operations permission group \n" +
		"\u2022 Return a Success if Consent Response is a 201 containing all permissions required on the scope of the test. Return a Warning and end the test if the consent request returns either a 422 or a 201 without Permission for this specific test.\n" +
		"\u2022 Redirect User to authorize the Created Consent - Expect a successful authorization\n" +
		"\u2022 With the authorized consent id (1), call the GET Unarranged Accounts List API Once - Expect a 200 - Save the first returned ACTIVE resource id (R_1) and the second saved returned active resource id (R_2)\n" +
		"\u2022 With the authorized consent id (1), call the GET Unarranged Accounts  API with the saved Resource ID (R_1) 30 times - Expect a 200\n" +
		"\u2022 With the authorized consent id (1), call the GET Unarranged Accounts Warranties API with the saved Resource ID (R_1) 30 times - Expect a 200\n" +
		"\u2022 With the authorized consent id (1), call the GET Unarranged Accounts Scheduled Instalments API with the saved Resource ID (R_1) 30 times - Expect a 200\n" +
		"\u2022 With the authorized consent id (1), call the GET Unarranged Accounts Payments API with the saved Resource ID (R_1) 30 times - Expect a 200\n" +
		"\u2022 With the authorized consent id (1), call the GET Unarranged Accounts API with the saved Resource ID (R_2) 30 times - Expect a 200\n" +
		"\u2022 Repeat the exact same process done with the first tested resources (R_1) but now, execute it against the second returned Resource (R_2) \n" +
		"\u2022 Using the regular client_id provided and the regular CPF/CNPJ for OL create a Consent Request sending the Credit Operations permission group - Expect Server to return a 201 - Save ConsentID (2)\n" +
		"\u2022 Redirect User to authorize the Created Consent - Expect a successful authorization\n" +
		"\u2022 With the authorized consent id (2), call the GET Unarranged Accounts List API Once - Expect a 200 - Save the first returned ACTIVE resource id (R_1) \n" +
		"\u2022 With the authorized consent id (2), call the GET Unarranged Accounts API with the saved Resource ID (R_1) once - Expect a 200 response\n" +
		"\u2022 Repeat the exact same process done with the first tested resources (R_1) done with the first client, now using the second client",
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
		"resource.brazilCpf",
		"resource.brazilCnpj",
		"resource.brazilCpfOperationalPersonal",
		"resource.brazilCpfOperationalBusiness",
		"resource.brazilCnpjOperationalBusiness",
		"consent.productType"
	}
)
public class UnarrangedAccountsApiOperationalLimitsTestModuleV2 extends AbstractOperationalLimitsTestModule {

	private static final String API_RESOURCE_ID = "contractId";
	private int numberOfIdsToFetch = 2;

	private int numberOfExecutions = 1;
	private ClientAuthType clientAuthType;


	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildCreditOperationsAdvancesConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddUnarrangedOverdraftScope.class);
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
		switchToSecondClient();
		callAndStopOnFailure(AddUnarrangedOverdraftScope.class);
		callAndContinueOnFailure(OperationalLimitsToConsentRequest.class);
		clientAuthType = getVariant(ClientAuthType.class);
		super.onConfigure(config, baseUrl);
	}

	@Override
	protected void validatePermissions() {
		env.putString("permission_type", EnsureSpecificCreditOperationsPermissionsWereReturned.CreditOperationsPermissionsType.UNARRANGED_ACCOUNTS_OVERDRAFT.name());
		callAndContinueOnFailure(EnsureSpecificCreditOperationsPermissionsWereReturned.class, Condition.ConditionResult.WARNING);
	}

	@Override
	protected void validateResponse() {
		//validate Unarranged Accounts response
		call(getValidationSequence(AdvancesResponseValidatorV2.class));
		eventLog.endBlock();

		runInBlock("Preparing Unarranged Accounts Contracts", () -> {
			env.putString("apiIdName", API_RESOURCE_ID);
			callAndStopOnFailure(ExtractAllSpecifiedApiIds.class);

			env.putInteger("number_of_ids_to_fetch", numberOfIdsToFetch);
			callAndStopOnFailure(FetchSpecifiedNumberOfExtractedApiIds.class);

			// Call Unarranged Accounts GET 29 times
			for (int i = 1; i < 30; i++) {
				preCallProtectedResource(String.format("[%d] Fetching Unarranged Accounts Contracts", i + 1));
			}

		});

		for (int i = 0; i < numberOfIdsToFetch; i++) {

			// Call Unarranged Accounts specific contract once with validation

			String unarrangedAccountsContractId = OIDFJSON.getString(env.getObject("fetched_api_ids").getAsJsonArray("fetchedApiIds").get(i));
			env.putString("contractId", unarrangedAccountsContractId);
			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContracts.class);

			preCallProtectedResource(String.format("Fetching Unarranged Accounts Contract using resource_id_%d and consent_id_%d", i + 1, numberOfExecutions));
			validateResponse("Validate Unarranged Accounts Contract Account response", AdvancesContractResponseValidatorV2.class);

			// Call Unarranged Accounts specific contract 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetching Unarranged Accounts Contract using resource_id_%d and consent_id_%d", j + 1, i + 1, numberOfExecutions));
			}

			// Call Unarranged Accounts warranties once with validation
			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractGuarantees.class);

			preCallProtectedResource(String.format("Fetch Unarranged Accounts Warranties using resource_id_%d and consent_id_%d", i + 1, numberOfExecutions));
			validateResponse("Validate Unarranged Accounts Warranties", AdvancesGuaranteesResponseValidatorV2.class);

			// Call Unarranged Accounts warranties 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Unarranged Accounts Warranties using resource_id_%d and consent_id_%d", j + 1, i + 1, numberOfExecutions));
			}

			// Call Unarranged Accounts Scheduled Instalments once with validation

			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractInstallments.class);

			preCallProtectedResource(String.format("Fetch Unarranged Accounts Scheduled Instalments using resource_id_%d and and consent_id_%d", i + 1, numberOfExecutions));
			validateResponse("Validate Unarranged Accounts Scheduled Instalments Response", AdvancesContractInstallmentsResponseValidatorV2.class);

			// Call Unarranged Accounts Scheduled Instalments 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Unarranged Accounts Scheduled Instalments using resource_id_%d and and consent_id_%d", j + 1, i + 1, numberOfExecutions));
			}

			refreshAccessToken();


			// Call Unarranged Accounts Payments GET once with validation

			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceContractPayments.class);

			preCallProtectedResource(String.format("Fetch Unarranged Accounts Payments using resource_id_%d and consent_id_%d", i + 1, numberOfExecutions));
			validateResponse("Validate Unarranged Accounts Payments Response", AdvancesPaymentsResponseValidatorV2.class);

			// Call Unarranged Accounts Payments GET 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Unarranged Accounts Payments using resource_id_%d and consent_id_%d", j + 1, i + 1, numberOfExecutions));
			}

		}

	}


	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (numberOfExecutions == 1) {
			callAndStopOnFailure(PrepareUrlForFetchingCreditAdvanceRoot.class);
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

	protected void validateResponse(String message, Class<? extends Condition> validationClass) {
		runInBlock(message, () -> call(getValidationSequence(validationClass)));
	}


	protected ConditionSequence getValidationSequence(Class<? extends Condition> validationClass) {
		return sequenceOf(
			condition(validationClass),
			condition(ValidateResponseMetaData.class)
		);
	}

	private void refreshAccessToken() {
		GenerateRefreshAccessTokenSteps refreshAccessTokenSteps = new GenerateRefreshAccessTokenSteps(clientAuthType);
		call(refreshAccessTokenSteps);
	}

}
