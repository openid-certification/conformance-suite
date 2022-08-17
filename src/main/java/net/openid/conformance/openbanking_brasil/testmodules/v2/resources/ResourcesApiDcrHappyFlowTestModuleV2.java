package net.openid.conformance.openbanking_brasil.testmodules.v2.resources;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddFAPIAuthDateToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddFAPIInteractionIdToResourceEndpointRequest;
import net.openid.conformance.condition.client.AddIpV4FapiCustomerIpAddressToResourceEndpointRequest;
import net.openid.conformance.condition.client.CallProtectedResource;
import net.openid.conformance.condition.client.CheckForDateHeaderInResourceResponse;
import net.openid.conformance.condition.client.CheckForFAPIInteractionIdInResourceResponse;
import net.openid.conformance.condition.client.CheckScopesFromDynamicRegistrationEndpointContainsOpenidResources;
import net.openid.conformance.condition.client.CreateEmptyResourceEndpointRequestHeaders;
import net.openid.conformance.condition.client.CreateRandomFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureMatchingFAPIInteractionId;
import net.openid.conformance.condition.client.EnsureResourceResponseReturnedJsonContentType;
import net.openid.conformance.openbanking_brasil.OBBProfile;
import net.openid.conformance.openbanking_brasil.generic.ErrorValidator;
import net.openid.conformance.openbanking_brasil.testmodules.AbstractApiDcrTestModule;
import net.openid.conformance.openbanking_brasil.testmodules.support.*;
import net.openid.conformance.openbanking_brasil.testmodules.support.warningMessages.CustomerDataResources404;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.FAPI1FinalOPProfile;
import net.openid.conformance.variant.VariantHidesConfigurationFields;

@PublishTestModule(
	testName = "resources-api-dcr-happyflow-v2",
	displayName = "Resources API V2 Use payments after registering client via DCR",
	summary = "Obtains a software statement from the Brazil sandbox directory (using a hardcoded client with the DADOS role), register a new client on the target authorization server and perform an authorization flow. Note that this test overrides the 'alias' value in the configuration, so you may see your test being interrupted if other users are testing.\n" +
		"\u2022 Using a hardcoded client with only the DADOS role, retrieves from the directory its SSA\n" +
		"\u2022 Performs a DCR on the provided authorization server -> Expects a successful registration which should also grant all Phase 2 scopes\n" +
		"\u2022 Performs a POST on the open-banking/consents/v1/consents endpoint -> Expects a successful creation of the consent request\n" +
		"\u2022 DELETEs the registered client from the authorization server",
	profile = OBBProfile.OBB_PROFILE,
	configurationFields = {
		"server.discoveryUrl"
	}
)
// hide various config values from the FAPI base module we don't need
@VariantHidesConfigurationFields(parameter = FAPI1FinalOPProfile.class, value = "openbanking_brazil", configurationFields = {
	"client.org_jwks",
	"resource.brazilOrganizationId",
	"resource.brazilPaymentConsent",
	"resource.brazilPixPayment"

})
public class ResourcesApiDcrHappyFlowTestModuleV2 extends AbstractApiDcrTestModule {


	@Override
	protected void configureClient() {
		callAndStopOnFailure(OverrideClientWithDadosClient.class);
		callAndStopOnFailure(OverrideScopeWithAllDadosScopes.class);
		callAndStopOnFailure(SetDirectoryInfo.class);
		callAndStopOnFailure(BuildCreditOperationsAdvancesConfigResourceUrlFromConsentUrl.class);

		super.configureClient();
	}

	@Override
	protected void validateDcrResponseScope() {
		// many banks don't support all phase2 scopes, so we may get fewer scopes than we requested
		callAndContinueOnFailure(CheckScopesFromDynamicRegistrationEndpointContainsOpenidResources.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1.1", "RFC7591-2", "RFC7591-3.2.1");
	}

	@Override
	protected void requestProtectedResource() {
		eventLog.startBlock(currentClientString() + "Resource server endpoint tests v2");

		ResourceApiV2PollingSteps pollingSteps = new ResourceApiV2PollingSteps(env, getId(),
			eventLog,testInfo, getTestExecutionManager());
		call(pollingSteps);

		String responseError = env.getString("resource_endpoint_error_code");
		if (!Strings.isNullOrEmpty(responseError)) {
			callAndContinueOnFailure(ErrorValidator.class, Condition.ConditionResult.FAILURE);
			callAndStopOnFailure(EnsureResponseCodeWas404.class);
			callAndStopOnFailure(CustomerDataResources404.class);
			callAndContinueOnFailure(ChuckWarning.class, Condition.ConditionResult.WARNING);
		}

		eventLog.endBlock();
	}

}
