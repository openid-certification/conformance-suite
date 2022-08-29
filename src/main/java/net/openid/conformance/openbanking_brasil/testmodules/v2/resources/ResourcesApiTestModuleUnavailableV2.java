package net.openid.conformance.openbanking_brasil.testmodules.v2.resources;

import com.google.gson.JsonObject;
import net.openid.conformance.ConditionSequenceRepeater;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.resourcesAPI.v1.PrepareUrlForResourcesCall;
import net.openid.conformance.openbanking_brasil.resourcesAPI.v2.*;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractOBBrasilFunctionalTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.creditOperations.financing.AddScopesForFinancingsApi;
import net.openid.conformance.openbanking_brasil.testmodules.customerAPI.AddScopesForCustomerApi;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.UnavailableResourcesApiPollingTimeout;
import net.openid.conformance.openbanking_brasil.testmodules.v2.GenerateRefreshAccessTokenSteps;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

import java.util.Optional;

@PublishTestModule(
	testName = "resources-api-test-unavailable-v2",
	displayName = "Validates that the server has correctly implemented the expected behaviour for temporarily blocked resources",
	summary = "Validates that the server has correctly implemented the expected behaviour for temporarily blocked resources\n" +
		"\u2022 Creates a CONSENT with all the existing permissions including either business or personal data, depending on what has been provided on the test configuration\n" +
		"\u2022 Expects a Success 201\n" +
		"\u2022 Redirect the user to authorize the CONSENT - Redirect URI must contain all phase 2 scopes\n" +
		"\u2022 Expect a Successful authorization with an authorization code created\n" +
		"\u2022 Call the RESOURCES API with the authorized consent\n" +
		"\u2022 Expect a 200 - Validate that AT LEAST one Resource has been returned and is on the state TEMPORARILY_UNAVAILABLE/UNAVAILABLE\n" +
		"\u2022 Evaluate which Resource is on the TEMPORARILY_UNAVAILABLE/UNAVAILABLE state, fetch the resource id, create the base request URI for said resource\n" +
		"\u2022 Call either the CONTRACTS or the ACCOUNTS list API for this Resource\n" +
		"\u2022 Expect a 200 - Make sure the Server returns a 200 without that TEMPORARILY_UNAVAILABLE/UNAVAILABLE resource on it's list\n" +
		"\u2022 Depending on the unavailable resource, call one of the following APIs depending: (1) /contracts/{contractId}/warranties for credit operations, (2) /accounts/{creditCardAccountId}/bills for credit cards, or (3) /accounts/{accountId}/balances for accounts\n" +
		"\u2022 Expect a 403 - Validate that the field response.errors.code is STATUS_RESOURCE_TEMPORARILY_UNAVAILABLE/STATUS_RESOURCE_UNAVAILABLE\n" +
		"\u2022 \n",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"resource.consentUrl",
		"resource.brazilCpf",
		"consent.productType"
	}
)
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks"
})
public class ResourcesApiTestModuleUnavailableV2 extends AbstractOBBrasilFunctionalTestModule {

	private ClientAuthType clientAuthType;
	@Override
	protected void configureClient() {
		callAndStopOnFailure(BuildResourcesConfigResourceUrlFromConsentUrl.class);
		super.configureClient();
	}

	@Override
	protected void onConfigure(JsonObject config, String baseUrl) {
		callAndStopOnFailure(AddAccountScope.class);
		callAndStopOnFailure(AddCreditCardScopes.class);
		callAndStopOnFailure(AddScopesForCustomerApi.class);
		callAndStopOnFailure(AddInvoiceFinancingsScope.class);
		callAndStopOnFailure(AddScopesForFinancingsApi.class);
		callAndStopOnFailure(AddLoansScope.class);
		callAndStopOnFailure(AddUnarrangedOverdraftScope.class);
		callAndStopOnFailure(AddResourcesScope.class);

		callAndStopOnFailure(PrepareAllResourceRelatedConsentsForHappyPathTest.class);
		callAndStopOnFailure(PrepareUrlForResourcesCall.class);

		clientAuthType = getVariant(ClientAuthType.class);
		super.onConfigure(config, baseUrl);
	}

	@Override
	protected void requestProtectedResource() {
		eventLog.startBlock(currentClientString() + "Resource server endpoint tests");
		callAndStopOnFailure(CreateEmptyResourceEndpointRequestHeaders.class);
		callAndStopOnFailure(AddFAPIAuthDateToResourceEndpointRequest.class);
		callAndStopOnFailure(AddIpV4FapiCustomerIpAddressToResourceEndpointRequest.class);
		callAndStopOnFailure(CreateRandomFAPIInteractionId.class);
		callAndStopOnFailure(AddFAPIInteractionIdToResourceEndpointRequest.class);
		callAndStopOnFailure(CallProtectedResource.class, "FAPI1-BASE-6.2.1-1", "FAPI1-BASE-6.2.1-3");
		call(exec().mapKey("endpoint_response", "resource_endpoint_response_full"));
		callAndStopOnFailure(CheckForDateHeaderInResourceResponse.class);
		callAndStopOnFailure(CheckForFAPIInteractionIdInResourceResponse.class);
		callAndStopOnFailure(EnsureMatchingFAPIInteractionId.class);
		callAndStopOnFailure(EnsureResourceResponseReturnedJsonContentType.class);

		eventLog.endBlock();

		eventLog.startBlock(currentClientString() + "Validate response");
		validateResponse();
		eventLog.endBlock();
	}
	@Override
	protected void validateResponse() {

		ResourceApiV2PollingSteps pollingSteps = new ResourceApiV2PollingSteps(env, getId(),
			eventLog,testInfo, getTestExecutionManager());
		runInBlock("Polling Resources API", () -> {
			call(pollingSteps);
		});

		callAndContinueOnFailure(ResourcesResponseValidatorV2.class, Condition.ConditionResult.FAILURE);

		ConditionSequenceRepeater sequenceRepeater = new ConditionSequenceRepeater(env, getId(), eventLog, testInfo, executionManager,
			() -> sequenceOf(condition(SaveUnavailableResourceData.class).dontStopOnFailure().onFail(Condition.ConditionResult.INFO)))
			.untilTrue("resource_found")
			.times(10)
			.trailingPause(60)
			.refreshSequence(new GenerateRefreshAccessTokenSteps(clientAuthType), 3)
			.onTimeout(sequenceOf(condition(UnavailableResourcesApiPollingTimeout.class),condition(ChuckWarning.class)));
		sequenceRepeater.run();

		runInBlock("Ensure we cannot see the given unavailable resource in its api list.", () -> {
			callAndStopOnFailure(UpdateSavedResourceData.class);
			callAndStopOnFailure(PrepareUrlForApiListForSavedResourceCall.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas200.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureUnavailableResourceIsNotOnList.class, Condition.ConditionResult.FAILURE);
		});

		runInBlock("Ensure we cannot access the unavailable resource.", () -> {
			callAndStopOnFailure(PrepareUrlForSavedResourceCall.class);
			callAndContinueOnFailure(CallProtectedResource.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureResponseCodeWas403.class);
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
		});
	}
}
