package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.AddScopeToDynamicRegistrationRequest;
import net.openid.conformance.condition.client.CheckScopesFromDynamicRegistrationEndpointDoNotExceedRequestedScopes;
import net.openid.conformance.condition.client.ReorderGrantTypesInDynamicRegistrationRequest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi2-security-profile-final-brazildcr-happy-flow-variant",
	displayName = "FAPI2-Security-Profile-Final: Brazil DCR happy flow variant",
	summary = "Obtain a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration), register a new client on the target authorization server and perform an authorization flow. The registration request has the members of the 'grant_types' in a different order to the normal happy flow test, and includes the optional 'scope' parameter.",
	profile = "FAPI2-Security-Profile-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase",
		"resource.resourceUrl"
	}
)
public class FAPI2SPFinalBrazilDCRHappyFlowVariant extends AbstractFAPI2SPFinalBrazilDCR {

	@Override
	protected void callRegistrationEndpoint() {
		callAndStopOnFailure(ReorderGrantTypesInDynamicRegistrationRequest.class, "RFC7591-2");

		callAndStopOnFailure(AddScopeToDynamicRegistrationRequest.class, "RFC7591-2");

		super.callRegistrationEndpoint();

		callAndContinueOnFailure(CheckScopesFromDynamicRegistrationEndpointDoNotExceedRequestedScopes.class, Condition.ConditionResult.FAILURE, "RFC7591-2", "RFC7591-3.2.1");
	}
}
