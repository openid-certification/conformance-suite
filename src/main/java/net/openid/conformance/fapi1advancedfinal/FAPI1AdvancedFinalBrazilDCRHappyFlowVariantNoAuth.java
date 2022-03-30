package net.openid.conformance.fapi1advancedfinal;

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
public class FAPI1AdvancedFinalBrazilDCRHappyFlowVariantNoAuth extends FAPI1AdvancedFinalBrazilDCRHappyFlow {

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		super.onPostAuthorizationFlowComplete();
	}

	@Override
	protected void setupResourceEndpoint() {
		// not needed as resource endpoint won't be called
	}

	@Override
	protected boolean scopeContains(String requiredScope) {
		// Not needed as scope field is optional
		return false;
	}

	@Override
	protected void validateDcrResponseScope() {
		// Not needed as scope field is optional
	}

	@Override
	protected void copyFromDynamicRegistrationTemplateToClientConfiguration() {
		// Not needed as scope field is optional
	}
}
