package net.openid.conformance.openbanking_brasil.testmodules.v2.operationalLimits;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.creditOperations.discountedCreditRights.v2.*;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.PrepareAllCreditOperationsPermissionsForHappyPath;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.discounted.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.OIDFJSON;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;


@PublishTestModule(
	testName = "discounted-credit-rights-api-operational-limits",
	displayName ="This test will make sure that the server is not blocking access to the APIs as long as the operational limits for the Financings API are considered correctly\n",
	summary = "This test will require the user to have set at least two ACTIVE resources on the Invoice Financings API. \n" +
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
public class InvoiceFinancingsApiOperationalLimitsTestModuleV2 extends AbstractOperationalLimitsTestModule {

	private static final String API_RESOURCE_ID = "contractId";
	private int numberOfIdsToFetch = 2;

	private int numberOfExecutions = 1;


	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildCreditOperationsDiscountedConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddInvoiceFinancingsScope.class);
		callAndStopOnFailure(PrepareAllCreditOperationsPermissionsForHappyPath.class);
		switchToSecondClient();
		callAndStopOnFailure(AddInvoiceFinancingsScope.class);
		callAndContinueOnFailure(OperationalLimitsToConsentRequest.class);
		clientAuthType = getVariant(ClientAuthType.class);
		super.onConfigure(config, baseUrl);
	}

	@Override
	protected void validatePermissions() {
		env.putString("permission_type", EnsureSpecificCreditOperationsPermissionsWereReturned.CreditOperationsPermissionsType.INVOICE_FINANCINGS.name());
		callAndContinueOnFailure(EnsureSpecificCreditOperationsPermissionsWereReturned.class, Condition.ConditionResult.WARNING);
	}

	@Override
	protected void validateResponse() {
		//validate invoice financings response
		call(getValidationSequence(InvoiceFinancingContractsResponseValidatorV2.class));
		eventLog.endBlock();

		runInBlock("Preparing Invoice Finanacings Contracts", () -> {
			env.putString("apiIdName", API_RESOURCE_ID);
			callAndStopOnFailure(ExtractAllSpecifiedApiIds.class);

			env.putInteger("number_of_ids_to_fetch", numberOfIdsToFetch);
			callAndStopOnFailure(FetchSpecifiedNumberOfExtractedApiIds.class);

		});

		for (int i = 0; i < numberOfIdsToFetch; i++) {
			int currentResourceId = i + 1;

			// Call invoice financings specific contract once with validation

			String invoiceFinancingsContractId = OIDFJSON.getString(env.getObject("fetched_api_ids").getAsJsonArray("fetchedApiIds").get(i));
			env.putString(API_RESOURCE_ID, invoiceFinancingsContractId);
			callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContract.class);

			preCallProtectedResource(String.format("Fetching Invoice Financings Contract using resource_id_%d and consent_id_%d", currentResourceId, numberOfExecutions));
			validateResponse("Validate Invoice Financings Contract response", InvoiceFinancingAgreementResponseValidatorV2.class);
			disableLogging();
			// Call invoice financings specific contract 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetching Invoice Financings Contract using resource_id_%d and consent_id_%d", j + 1, currentResourceId, numberOfExecutions));
			}

			// Call invoice financings warranties once with validation
			runInLoggingBlock(() -> {
				callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractGuarantees.class);

				preCallProtectedResource(String.format("Fetch Invoice Financings Warranties using resource_id_%d and consent_id_%d", currentResourceId, numberOfExecutions));
				validateResponse("Validate Invoice Financings Warranties", InvoiceFinancingContractGuaranteesResponseValidatorV2.class);

			});

			// Call invoice financings warranties 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Invoice Financings Warranties using resource_id_%d and consent_id_%d", j + 1, currentResourceId, numberOfExecutions));
			}

			// Call invoice financings Scheduled Instalments once with validation

			runInLoggingBlock(() -> {
				callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractInstalments.class);

				preCallProtectedResource(String.format("Fetch Invoice Financings Scheduled Instalments using resource_id_%d and and consent_id_%d", currentResourceId, numberOfExecutions));
				validateResponse("Validate Invoice Financings Scheduled Instalments Response", InvoiceFinancingContractInstallmentsResponseValidatorV2.class);

			});

			// Call invoice financings Scheduled Instalments 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Invoice Financings Scheduled Instalments using resource_id_%d and and consent_id_%d", j + 1, currentResourceId, numberOfExecutions));
			}

			refreshAccessToken();


			// Call invoice financings Payments GET once with validation

			runInLoggingBlock(() -> {
				callAndStopOnFailure(PrepareUrlForFetchingCreditDiscountedCreditRightsContractPayments.class);

				preCallProtectedResource(String.format("Fetch Invoice Financings Payments using resource_id_%d and consent_id_%d", currentResourceId, numberOfExecutions));
				validateResponse("Validate Invoice Financings Payments Response", InvoiceFinancingContractPaymentsResponseValidatorV2.class);

			});

			// Call invoice financings Payments GET 29 times
			for (int j = 1; j < 30; j++) {
				preCallProtectedResource(String.format("[%d] Fetch Invoice Financings Payments using resource_id_%d and consent_id_%d", j + 1, currentResourceId, numberOfExecutions));
			}
			enableLogging();
		}

	}


	@Override
	protected void onPostAuthorizationFlowComplete() {
		enableLogging();
		if (numberOfExecutions == 1) {
			callAndStopOnFailure(PrepareUrlForDiscountedRoot.class);
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

}
