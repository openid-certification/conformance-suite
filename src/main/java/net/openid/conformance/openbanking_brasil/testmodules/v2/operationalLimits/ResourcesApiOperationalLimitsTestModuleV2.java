package net.openid.conformance.openbanking_brasil.testmodules.v2.operationalLimits;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.resourcesAPI.v2.ResourcesResponseValidatorV2;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.OpenBankingBrazilPreAuthorizationSteps;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "resources-api-operational-limits",
	displayName = "Test will make sure that the server has not implemented any type of operational limits for the Resources API.",
	summary = "The test will require a DCR to be executed prior to the test against a server whose credentials are provided here XPTO.\n" +
		"This test will generate three different consent requests and call the resources API 15 times for each created consent\n" +
		"Test will make sure that the server has not implemented any type of operational limits for the Resources API.\n" +
		"\u2022 Make Sure that the fields “Client_id for Operational Limits Test” (client_id for OL) and at least the CPF for Operational Limits (CPF for OL) test has been provided\n" +
		"\u2022 Using the client_id for OL and the CPF/CNPJ for OL create a Consent Request sending either business or customer permissions, depending on what has been provided on the test plan configuration - Expect Server to return a 201 - Save ConsentID (1)\n" +
		"\u2022 Return a Success if Consent Response is a 201 containing all permissions required on the scope of the test. Return a Warning and end the test if the consent request returns either a 422 or a 201 without Permission for this specific test.\n" +
		"\u2022 With the authorized consent id (1) , call the GET Resources API 15 Times - Expect a 200 on all requests\n" +
		"\u2022 Using the client_id for OL and the CPF/CNPJ for OL create a Consent Request sending either business or customer permissions, depending on what has been provided on the test plan configuration - Expect Server to return a 201 - Save ConsentID (2)\n" +
		"\u2022 Redirect User to authorize the Created Consent - Expect a successful authorization\n" +
		"\u2022 With the authorized consent id (2) , call the GET Resources API 15 Times - Expect a 200\n" +
		"\u2022 Using the regular client_id provided and the regular CPF/CNPJ for OL create a Consent Request sending either business or customer permissions, depending on what has been provided on the test plan configuration - Expect Server to return a 201 - Save ConsentID (3)\n" +
		"\u2022 Redirect User to authorize the Created Consent - Expect a successful authorization\n" +
		"\u2022 With the authorized consent id (3), call the GET Resources API 15 Times - Expect a 200 on all request",
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
public class ResourcesApiOperationalLimitsTestModuleV2 extends AbstractOBBrasilFunctionalTestModule {

	private int numberOfExecutions = 1;


	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildResourcesConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(PrepareAllResourceRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(AddResourcesScope.class);
		callAndStopOnFailure(EnsureClientIdForOperationalLimitsIsPresent.class);
		callAndStopOnFailure(SwitchToOperationalLimitsClient.class);
		callAndContinueOnFailure(OperationalLimitsToConsentRequest.class);
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
		}

		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE);
		call(exec().unmapKey("endpoint_response"));
		callAndContinueOnFailure(FAPIBrazilConsentEndpointResponseValidatePermissions.class, Condition.ConditionResult.WARNING);

		if (getResult() == Result.WARNING) {
			fireTestFinished();
		}

		callAndContinueOnFailure(EnsureResponseHasLinksForConsents.class, Condition.ConditionResult.FAILURE);
		callAndContinueOnFailure(ValidateResponseMetaData.class, Condition.ConditionResult.FAILURE);
		callAndStopOnFailure(ExtractConsentIdFromConsentEndpointResponse.class);
		callAndContinueOnFailure(CheckForFAPIInteractionIdInResourceResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-6.2.1-11", "FAPI1-BASE-6.2.1-11");
		callAndStopOnFailure(FAPIBrazilAddConsentIdToClientScope.class);

	}



	@Override
	protected void requestProtectedResource() {
		for (int i = 0; i < 15; i++) {
			super.requestProtectedResource();
			validationStarted = false;
		}

	}


	@Override
	protected void onPostAuthorizationFlowComplete() {
		expose("consnet_id_" + numberOfExecutions, env.getString("consent_id"));

		if(numberOfExecutions == 3){
			fireTestFinished();
		}else {

			if(numberOfExecutions == 2){
				callAndStopOnFailure(SwitchToOriginalClient.class);
				callAndStopOnFailure(RemoveOperationalLimitsFromConsentRequest.class);
			}

			callAndContinueOnFailure(RemoveConsentIdFromClientScopes.class);
			performAuthorizationFlow();
			numberOfExecutions++;
		}
	}

	@Override
	protected void validateResponse() {
		callAndContinueOnFailure(EnsureResponseCodeWas200.class);
		callAndContinueOnFailure(ValidateResponseMetaData.class);
		callAndStopOnFailure(ResourcesResponseValidatorV2.class);
	}
}
