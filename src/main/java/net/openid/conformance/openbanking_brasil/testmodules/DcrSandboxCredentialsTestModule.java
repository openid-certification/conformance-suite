package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.ClientManagementEndpointAndAccessTokenRequired;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalBrazilDCRNoSoftwareStatement;
import net.openid.conformance.openbanking_brasil.testmodules.support.ReplaceByHardcodedSandboxCredentials;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "dcr-test-sandbox-credentials",
	displayName = "FAPI1-Advanced-Final: Brazil DCR no software statement",
	summary = "Perform the DCR flow, but without including a software statement (the values in the software statement are added to the body of the request) - the server must reject the registration attempt. " +
		"Set hardcoded credentials from Sandbox Environment",
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
public class DcrSandboxCredentialsTestModule extends FAPI1AdvancedFinalBrazilDCRNoSoftwareStatement {

	@Override
	protected void configureClient() {
		callAndStopOnFailure(ReplaceByHardcodedSandboxCredentials.class);
		super.configureClient();
	}
	@Override
	protected boolean scopeContains(String requiredScope) {
		// Not needed as scope field is optional
		return false;
	}

}
