package net.openid.conformance.openbanking_brasil.testmodules.v2.operationalLimits;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.registrationData.v2.BusinessIdentificationValidatorV2;
import net.openid.conformance.openbanking_brasil.registrationData.v2.BusinessQualificationResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.registrationData.v2.BusinessRelationsResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.testmodule.PublishTestModule;


@PublishTestModule(
	testName = "customer-business-api-operational-limits",
	displayName = "Make sure that the server is not blocking access to the APIs as long as the operational limits for the Customer Business API are considered correctly",
	summary ="This test will make sure that the server is not blocking access to the APIs as long as the operational limits for the Customer Business API are considered correctly.\n" +
		"\u2022 Make Sure that the fields “Client_id for Operational Limits Test” (client_id for OL) and at least the CPF for Operational Limits (CPF for OL) test have been provided\n" +
		"\u2022 Using the HardCoded clients provided on the test summary link, use the client_id for OL and the CPF/CNPJ for OL passed on the configuration and create a Consent Request sending the Customer Business permission group - Expect Server to return a 201 - Save ConsentID (1)\n" +
		"\u2022 Return a Success if Consent Response is a 201 containing all permissions required on the scope of the test. Return a Warning and end the test if the consent request returns either a 422 or a 201 without Permission for this specific test.\n" +
		"\u2022 With the authorized consent id (1), call the GET Customer Business Identifications API 30 Times - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (1), call the GET Customer Business Qualifications 30 Times - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (1), call the GET Customer Business Financial Relations 30 Times - Expect a 200 response\n" +
		"\u2022 Using the regular client_id provided and the regular CPF/CNPJ for OL create a Consent Request sending the Customer Business permission group - Expect Server to return a 201 - Save ConsentID (2)\n" +
		"\u2022 Redirect User to authorize the Created Consent - Expect a successful authorization\n" +
		"\u2022 With the authorized consent id (2), call the GET Customer Business Identifications API 30 Times - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (2), call the GET Customer Business Qualifications 30 Times - Expect a 200 response\n" +
		"\u2022 With the authorized consent id (2), call the GET Customer Business Financial Relations 30 Times - Expect a 200 response",
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
		"resource.brazilCpfBusiness",
		"resource.brazilCnpjBusiness",
		"resource.brazilCpfOperationalBusiness",
		"resource.brazilCnpjOperationalBusiness"
	}
)
public class CustomerBusinessApiOperationalLimitsTestModuleV2 extends AbstractOperationalLimitsTestModule {

	private int numberOfExecutions = 1;

	@Override
	protected void configureClient() {
		callAndContinueOnFailure(BuildBusinessCustomersConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndStopOnFailure(PrepareAllCustomerBusinessRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(AddDummyBusinessProductTypeToConfig.class);
		switchToSecondClient();
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndContinueOnFailure(OperationalLimitsToConsentRequest.class);
	}


	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		return new OpenBankingBrazilPreAuthorizationSteps(isSecondClient(), false, addTokenEndpointClientAuthentication, brazilPayments.isTrue(), true);
	}

	@Override
	protected void validateResponse() {
		// Validate Business Identification response
		callAndStopOnFailure(BusinessIdentificationValidatorV2.class);
		callAndStopOnFailure(ValidateResponseMetaData.class);

		eventLog.endBlock();

		// Call Business Identification 29 times
		disableLogging();
		for (int i = 1; i < 30; i++) {
			preCallProtectedResource(String.format("[%d] Calling Business Identification Endpoint with consent_id_%d", i + 1, numberOfExecutions));
		}

		// Call Business Qualifications once with validation
		runInLoggingBlock(() -> {
			callAndStopOnFailure(PrepareToGetBusinessQualifications.class);

			preCallProtectedResource(String.format("Calling Business Qualifications Endpoint with consent_id_%d", numberOfExecutions));
			validateResponse("Validate Business Qualifications response", BusinessQualificationResponseValidatorV2.class);
		});

		// Call Business Qualifications 29 times
		for (int i = 1; i < 30; i++) {
			preCallProtectedResource(String.format("[%d] Calling Business Qualifications Endpoint with consent_id_%d", i + 1, numberOfExecutions));
		}


		// Call Customer Business Financial Relations once with validation
		runInLoggingBlock(() -> {
			callAndStopOnFailure(PrepareToGetBusinessFinancialRelations.class);

			preCallProtectedResource(String.format("Calling Customer Business Financial Relations Endpoint with consent_id_%d", numberOfExecutions));
			validateResponse("Validate Customer Business Financial Relations response", BusinessRelationsResponseValidatorV2.class);

		});

		// Call Customer Business Financial Relations 29 times
		for (int i = 1; i < 30; i++) {
			preCallProtectedResource(String.format("[%d] Calling Customer Business Financial Relations Endpoint with consent_id_%d", i + 1, numberOfExecutions));
		}

	}

	private void validateResponse(String message, Class<? extends Condition> validator) {
		runInBlock(message, () -> {
			callAndStopOnFailure(validator);
			callAndStopOnFailure(ValidateResponseMetaData.class);
		});
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		enableLogging();
		if (numberOfExecutions == 1) {
			callAndStopOnFailure(PrepareToGetBusinessIdentifications.class);
			unmapClient();
			callAndStopOnFailure(RemoveOperationalLimitsFromConsentRequest.class);
			callAndStopOnFailure(RemoveConsentIdFromClientScopes.class);
			validationStarted = false;
			numberOfExecutions++;
			performAuthorizationFlow();
		} else {
			fireTestFinished();
		}
	}
}
