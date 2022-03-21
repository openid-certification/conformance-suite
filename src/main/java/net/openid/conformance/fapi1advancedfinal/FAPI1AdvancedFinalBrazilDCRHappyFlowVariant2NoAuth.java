package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazildcr-happy-flow-variant-2-no-authorization-flow",
	displayName = "FAPI1-Advanced-Final: Brazil DCR happy flow variant 2 without authentication flow",
	summary = "\u2022 Obtains a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration)\n" +
		"\u2022 Registers a new client on the target authorization server.\n" +
		"\u2022 The registration request has the members of the 'scope' string in a different order to the other happy flow variant test.",
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
public class FAPI1AdvancedFinalBrazilDCRHappyFlowVariant2NoAuth extends FAPI1AdvancedFinalBrazilDCRHappyFlowVariant2{
	@Override
	public void start() {
		fireTestFinished();
	}
}
