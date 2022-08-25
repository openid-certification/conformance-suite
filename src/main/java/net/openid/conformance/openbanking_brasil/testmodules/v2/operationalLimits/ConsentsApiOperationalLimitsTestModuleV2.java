package net.openid.conformance.openbanking_brasil.testmodules.v2.operationalLimits;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.openid.conformance.AbstractFunctionalTestModule;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.consent.v2.ConsentDetailsIdentifiedByConsentIdValidatorV2;
import net.openid.conformance.openbanking_brasil.consent.v2.CreateNewConsentValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.account.BuildAccountsConfigResourceUrlFromConsentUrl;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.consent.v2.OpenBankingBrazilPreAuthorizationConsentApiV2;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.util.JsonUtils;

@PublishTestModule(
	testName = "consents-api-operational-limits",
	displayName = "Consents Api operational limits test module V2",
	summary = "Make sure that access is not blocked on the Consents API V2 regardless of the number of calls done against it.\n\n" +
		"\u2022 Create a consent using the CPF and CNPJ provided for the Operational Limits tests. Send the permissions for either customer business or customer personal data, based on what has been provided on the test configuration\n" +
		"\u2022 Redirect the user to authorise the Consent with the customer and the created consent scopes- Expect a success on the redirect\n" +
		"\u2022 Call the GET Consents API 600 Times using the Authorized ConsentID\n" +
		"\u2022 Expect every single call to return a 600 times - Expect a 200 - response_body should only be validated on first API Call\n",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"consent.productType",
		"resource.brazilCpfOperationalPersonal",
		"resource.brazilCpfOperationalBusiness",
		"resource.brazilCnpjOperationalBusiness"
	}
)
public class ConsentsApiOperationalLimitsTestModuleV2 extends AbstractOperationalLimitsTestModule {

	private static final int NUMBER_OF_EXECUTIONS = 600;

	@Override
	protected void configureClient() {
		//Arbitrary resource
		callAndStopOnFailure(BuildAccountsConfigResourceUrlFromConsentUrl.class);
		callAndStopOnFailure(PrepareAllCustomerRelatedConsentsForResource404HappyPathTest.class);
		super.configureClient();
	}

	@Override
	protected void validateSecondClient() {
		// Only normal client is used in this test
	}

	@Override
	protected void validateClientConfiguration() {
		super.validateClientConfiguration();
		callAndStopOnFailure(AddConsentScope.class);
		callAndContinueOnFailure(OperationalLimitsToConsentRequest.class);
	}

	@Override
	protected ConditionSequence createOBBPreauthSteps() {
		env.putString("proceed_with_test", "true");
		ConditionSequence preauthSteps = new OpenBankingBrazilPreAuthorizationConsentApiV2(addTokenEndpointClientAuthentication, true);
		return preauthSteps;
	}


	@Override
	protected void performPreAuthorizationSteps() {

		call(createOBBPreauthSteps());

		callAndContinueOnFailure(EnsureResourceEndpointResponseStatusWas201.class, Condition.ConditionResult.WARNING);

		if (getResult() == Result.WARNING) {
			fireTestFinished();
		} else {
			callAndContinueOnFailure(EnsureResourceResponseReturnedJsonContentType.class, Condition.ConditionResult.FAILURE);

			String responseJson = env.getString("resource_endpoint_response");
			Gson gson = JsonUtils.createBigDecimalAwareGson();
			env.putObject("resource_endpoint_response", gson.fromJson(responseJson, JsonObject.class));

			env.mapKey("consent_endpoint_response", "resource_endpoint_response");
			callAndContinueOnFailure(FAPIBrazilConsentEndpointResponseValidatePermissions.class, Condition.ConditionResult.WARNING);

			if (getResult() == Result.WARNING) {
				fireTestFinished();
			} else {
				env.unmapKey("consent_endpoint_response");


				callAndContinueOnFailure(CreateNewConsentValidatorV2.class, Condition.ConditionResult.FAILURE);
				callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
				callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
				callAndContinueOnFailure(CheckItemCountHasMin1.class);

				call(exec().startBlock("Validating get consent response"));
				callAndStopOnFailure(ConsentIdExtractor.class);
				callAndStopOnFailure(PrepareToFetchConsentRequest.class);
				callAndContinueOnFailure(CallConsentApiWithBearerToken.class, Condition.ConditionResult.FAILURE);
				callAndContinueOnFailure(ConsentDetailsIdentifiedByConsentIdValidatorV2.class, Condition.ConditionResult.FAILURE);
				callAndContinueOnFailure(EnsureResponseHasLinks.class, Condition.ConditionResult.REVIEW);
				callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.REVIEW);
				callAndStopOnFailure(FAPIBrazilAddConsentIdToClientScope.class);

				callAndStopOnFailure(RemoveConsentScope.class);
			}

		}

	}

	@Override
	protected void requestProtectedResource() {
		for (int i = 0; i < NUMBER_OF_EXECUTIONS; i++) {
			if (i % 100 == 0) {
				enableLogging();
				eventLog.startBlock(currentClientString() + "Refreshing access token.");
				call(createGetAccessTokenWithClientCredentialsSequence(addTokenEndpointClientAuthentication));
				eventLog.endBlock();
				disableLogging();
			}

			eventLog.startBlock(currentClientString() + String.format("[%d] Calling consent endpoint.", i + 1));
			call(getPreConsentWithBearerTokenSequence());
			eventLog.endBlock();

			if (i == 0) {
				call(getValidateConsentResponsePollingSequence());
				disableLogging();
			}
		}
	}

	@Override
	protected void validateResponse() {
	}

	protected ConditionSequence createGetAccessTokenWithClientCredentialsSequence(Class<? extends ConditionSequence> clientAuthSequence) {
		return new ObtainAccessTokenWithClientCredentials(clientAuthSequence);
	}

	protected ConditionSequence getValidateConsentResponsePollingSequence() {
		return sequenceOf(
			condition(ConsentDetailsIdentifiedByConsentIdValidatorV2.class),
			condition(EnsureConsentResponseWas200.class)
		);
	}

	protected ConditionSequence getPreConsentWithBearerTokenSequence() {
		return sequenceOf(
			condition(ConsentIdExtractor.class),
			condition(PrepareToFetchConsentRequest.class),
			condition(CallConsentApiWithBearerToken.class)
		);
	}
}
