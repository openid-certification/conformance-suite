package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.client.ReorderGrantTypesInDynamicRegistrationRequest;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazildcr-happy-flow-variant",
	displayName = "FAPI1-Advanced-Final: Brazil DCR happy flow variant",
	summary = "Obtain a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration), register a new client on the target authorization server and perform an authorization flow. The registration request has slight (but valid) differents to the normal happy flow test.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.discoveryUrl",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"directory.discoveryUrl",
		"directory.client_id",
		"directory.apibase",
		"resource.resourceUrl"
	}
)
public class FAPI1AdvancedFinalBrazilDCRHappyFlowVariant extends AbstractFAPI1AdvancedFinalBrazilDCR {

	@Override
	protected void callRegistrationEndpoint() {
		callAndStopOnFailure(ReorderGrantTypesInDynamicRegistrationRequest.class);

		super.callRegistrationEndpoint();
	}
}
