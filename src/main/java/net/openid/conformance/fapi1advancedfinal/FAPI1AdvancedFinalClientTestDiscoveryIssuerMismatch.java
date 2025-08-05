package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.as.ChangeIssuerInServerConfigurationToBeInvalid;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-client-test-discovery-issuer-mismatch",
	displayName = "FAPI1-Advanced-Final: Relying party OpenID discovery issuer mismatch",
	summary = "The client is expected to retrieve OpenID Provider Configuration Information from the .well-known/openid-configuration endpoint and detect that the issuer in the provider configuration does not match the Issuer URL that was used as the prefix to /.well-known/openid-configuration to retrieve the configuration information. As per OpenID Connect Discovery Section 4.3 the client is expected to stop the flow.",
	profile = "FAPI1-Advanced-Final",
	configurationFields = {
		"server.jwks",
		"client.client_id",
		"client.scope",
		"client.redirect_uri",
		"client.certificate",
		"client.jwks"
	}
)

public class FAPI1AdvancedFinalClientTestDiscoveryIssuerMismatch extends AbstractFAPI1AdvancedFinalClientTest {

	@Override
	protected void onConfigurationCompleted() {
		super.onConfigurationCompleted();

		// Add invalid issuer to server configuration.
		callAndStopOnFailure(ChangeIssuerInServerConfigurationToBeInvalid.class);
	}

	@Override
	protected Object discoveryEndpoint() {
		Object returnValue = super.discoveryEndpoint();
		startWaitingForTimeout();
		return returnValue;
	}

	@Override
	protected void addCustomValuesToIdToken(){
		//Do nothing
	}

}
