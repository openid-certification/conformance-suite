package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddScopeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CheckScopesFromDynamicRegistrationEndpointDoNotExceedRequestedScopes;
import net.openid.conformance.condition.client.ClientManagementEndpointAndAccessTokenRequired;
import net.openid.conformance.condition.client.ReorderGrantTypesInDynamicRegistrationRequest;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddSupportedOpenIdScopesToClientConfig;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazildcr-happy-flow-variant-no-authorization-flow",
	displayName = "FAPI1-Advanced-Final: Brazil DCR happy flow variant without authentication flow",
	summary = "\u2022 Obtains a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration).\n" +
		"\u2022 Registers a new client on the target authorization server.\n" +
		"\u2022 The registration request has the members of the 'grant_types' in a different order to the normal happy flow test, and includes the optional 'scope' parameter.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase"
	}
)
public class FAPI1AdvancedFinalBrazilDCRHappyFlowVariantNoAuth extends AbstractFAPI1AdvancedFinalBrazilDCR{

	@Override
	protected void callRegistrationEndpoint() {
		callAndStopOnFailure(AddSupportedOpenIdScopesToClientConfig.class);

		callAndStopOnFailure(ReorderGrantTypesInDynamicRegistrationRequest.class, "RFC7591-2");

		callAndStopOnFailure(AddScopeToDynamicRegistrationRequest.class, "RFC7591-2");

		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));
		callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");
		validateDcrResponseScope();
		eventLog.endBlock();

		callAndContinueOnFailure(CheckScopesFromDynamicRegistrationEndpointDoNotExceedRequestedScopes.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-", "RFC7591-2", "RFC7591-3.2.1");
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		super.onPostAuthorizationFlowComplete();
	}

	@Override
	protected void setupResourceEndpoint() {
		// not needed as resource endpoint won't be called
	}

}
