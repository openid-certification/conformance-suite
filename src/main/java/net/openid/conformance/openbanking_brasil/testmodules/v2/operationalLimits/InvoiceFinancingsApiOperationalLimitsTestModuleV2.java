package net.openid.conformance.openbanking_brasil.testmodules.v2.operationalLimits;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.v2.FinancingContractResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.v2.FinancingGuaranteesResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.v2.FinancingPaymentsResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.creditOperations.financing.v2.FinancingResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.creditOperations.loans.v2.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.*;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.loans.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.payments.GenerateRefreshTokenRequest;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;


@PublishTestModule(
	testName = "discounted-credit-rights-api-operational-limits",
	displayName = "The test will require a DCR to be executed prior to the test against a server whose credentials are provided here https://gitlab.com/obb1/certification/-/wikis/Operational-Limits\n" +
		"This test will require the user to have set at least two ACTIVE resources on the Invoice Financings API. \n" +
		"This test will make sure that the server is not blocking access to the APIs as long as the operational limits for the Invoice Financings API are considered correctly.\n" +
		"\u2022 Make Sure that the fields “Client_id for Operational Limits Test” (client_id for OL) and at least the CPF for Operational Limits (CPF for OL) test have been provided\n" +
		"\u2022 Using the HardCoded clients provided on the test summary link, use the client_id for OL and the CPF/CNPJ for OL passed on the configuration and create a Consent Request sending the Credit Operations permission group\n" +
		"\u2022 Return a Success if Consent Response is a 201 containing all permissions required on the scope of the test. Return a Warning and end the test if the consent request returns either a 422 or a 201 without Permission for this specific test.\n" +
		"\u2022 Redirect User to authorize the Created Consent - Expect a successful authorization\n" +
		"\u2022 With the authorized consent id (1), call the GET Invoice Financings List API Once - Expect a 200 - Save the first returned ACTIVE resource id (R_1) and the second saved returned active resource id (R_2)\n" +
		"\u2022 With the authorized consent id (1), call the GET Invoice Financings  API with the saved Resource ID (R_1) 30 times - Expect a 200\n" +
		"\u2022 With the authorized consent id (1), call the GET Invoice Financings Warranties API with the saved Resource ID (R_1) 30 times - Expect a 200\n" +
		"\u2022 With the authorized consent id (1), call the GET Invoice Financings Scheduled Instalments API with the saved Resource ID (R_1) 30 times - Expect a 200\n" +
		"\u2022 With the authorized consent id (1), call the GET Invoice Financings Payments API with the saved Resource ID (R_1) 30 times - Expect a 200\n" +
		"\u2022 With the authorized consent id (1), call the GET Invoice Financings API with the saved Resource ID (R_2) 30 times - Expect a 200\n" +
		"\u2022 Repeat the exact same process done with the first tested resources (R_1) but now, execute it against the second returned Resource (R_2) \n" +
		"\u2022 Using the regular client_id provided and the regular CPF/CNPJ for OL create a Consent Request sending the Credit Operations permission group - Expect Server to return a 201 - Save ConsentID (2)\n" +
		"\u2022 Redirect User to authorize the Created Consent - Expect a successful authorization\n" +
		"\u2022 With the authorized consent id (2), call the GET Invoice Financings List API Once - Expect a 200 - Save the first returned ACTIVE resource id (R_1) \n" +
		"\u2022 With the authorized consent id (2), call the GET Invoice Financings API with the saved Resource ID (R_1) once - Expect a 200 response\n" +
		"\u2022 Repeat the exact same process done with the first tested resources (R_1) done with the first client, now using the second client",
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
public class InvoiceFinancingsApiOperationalLimitsTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {

	private static final String API_RESOURCE_ID = "contractId";
	private int numberOfIdsToFetch = 2;

	private int numberOfExecutions = 1;
	private ClientAuthType clientAuthType;


	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildFinancingsConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddInvoiceFinancingsScope.class);
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
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
		} else {
			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE);
			call(exec().unmapKey("endpoint_response"));

			env.putString("permission_type", EnsureSpecificCreditOperationsPermissionsWereReturned.CreditOperationsPermissionsType.INVOICE_FINANCINGS.name());
			callAndContinueOnFailure(EnsureSpecificCreditOperationsPermissionsWereReturned.class, Condition.ConditionResult.WARNING);

			if (getResult() == Result.WARNING) {
				fireTestFinished();
			} else {
				callAndContinueOnFailure(EnsureResponseHasLinksForConsents.class, Condition.ConditionResult.FAILURE);
				callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
				callAndStopOnFailure(ExtractConsentIdFromConsentEndpointResponse.class);
				callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11", "FAPI1-BASE-6.2.1-11");
				callAndStopOnFailure(FAPIBrazilAddConsentIdToClientScope.class);
			}

		}

	}

	@Override
	protected void validateResponse() {
		//validate invoice financings response
		call(getValidationSequence(FinancingResponseValidatorV2.class));
		eventLog.endBlock();

		runInBlock("Preparing Invoice Finanacings Contracts", () -> {
			env.putString("apiIdName", API_RESOURCE_ID);
			callAndStopOnFailure(ExtractAllSpecifiedApiIds.class);

			env.putInteger("number_of_ids_to_fetch", numberOfIdsToFetch);
			callAndStopOnFailure(FetchSpecifiedNumberOfExtractedApiIds.class);

		});

		for (int i = 0; i < numberOfIdsToFetch; i++) {

			// Call invoice financings specific contract once with validation

			String loanContractId = OIDFJSON.getString(env.getObject("fetched_api_ids").getAsJsonArray("fetchedApiIds").get(i));
			env.putString("contractId", loanContractId);
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractResource.class);

			preCallProtectedResource(String.format("Fetching Invoice Financings Contract using resource_id_%d and consent_id_%d", i + 1, numberOfExecutions));
			validateResponse("Validate Invoice Financings Contract response", FinancingContractResponseValidatorV2.class);

			// Call invoice financings specific contract 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetching Invoice Financings Contract using resource_id_%d and consent_id_%d", j + 1, i + 1, numberOfExecutions));
			}

			// Call invoice financings warranties once with validation
			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractWarrantiesResource.class);

			preCallProtectedResource(String.format("Fetch Invoice Financings Warranties using resource_id_%d and consent_id_%d", i + 1, numberOfExecutions));
			validateResponse("Validate Invoice Financings Warranties", FinancingGuaranteesResponseValidatorV2.class);

			// Call invoice financings warranties 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Invoice Financings Warranties using resource_id_%d and consent_id_%d", j + 1, i + 1, numberOfExecutions));
			}

			// Call invoice financings Scheduled Instalments once with validation

			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractInstallmentsResource.class);

			preCallProtectedResource(String.format("Fetch Invoice Financings Scheduled Instalments using resource_id_%d and and consent_id_%d", i + 1, numberOfExecutions));
			validateResponse("Validate Invoice Financings Scheduled Instalments Response", ContractInstallmentsResponseValidatorV2.class);

			// Call Loans Scheduled Instalments 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Invoice Financings Scheduled Instalments using resource_id_%d and and consent_id_%d", j + 1, i + 1, numberOfExecutions));
			}

			refreshAccessToken();


			// Call invoice financings Payments GET once with validation

			callAndStopOnFailure(PrepareUrlForFetchingFinancingContractPaymentsResource.class);

			preCallProtectedResource(String.format("Fetch Invoice Financings Payments using resource_id_%d and consent_id_%d", i + 1, numberOfExecutions));
			validateResponse("Validate Invoice Financings Payments Response", FinancingPaymentsResponseValidatorV2.class);

			// Call Loans Payments GET 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Invoice Financings Payments using resource_id_%d and consent_id_%d", j + 1, i + 1, numberOfExecutions));
			}

		}

	}


	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (numberOfExecutions == 1) {
			callAndStopOnFailure(PrepareUrlForFinancingRoot.class);
			callAndStopOnFailure(SwitchToOriginalClient.class);
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
		runInBlock("Refreshing Access Token", () -> call(getRefreshAccessTokenSequence()));
	}


	private ConditionSequence getRefreshAccessTokenSequence() {
		ConditionSequence sequence = sequenceOf(
			condition(GenerateRefreshTokenRequest.class),
			condition(CreateClientAuthenticationAssertionClaims.class),
			condition(SignClientAuthenticationAssertion.class),
			condition(AddClientAssertionToTokenEndpointRequest.class),
			condition(CallTokenEndpoint.class),
			condition(CheckIfTokenEndpointResponseError.class),
			condition(CheckForAccessTokenValue.class),
			condition(ExtractAccessTokenFromTokenResponse.class)

		);
		if (clientAuthType == ClientAuthType.MTLS) {
			sequence.insertAfter(SetPaymentsScopeOnTokenEndpointRequest.class, condition(AddClientIdToTokenEndpointRequest.class))
				.skip(CreateClientAuthenticationAssertionClaims.class, "Skipping step for MTLS")
				.skip(SignClientAuthenticationAssertion.class, "Skipping step for MTLS")
				.skip(AddClientAssertionToTokenEndpointRequest.class, "Skipping step for MTLS");
		}
		return sequence;
	}

}
