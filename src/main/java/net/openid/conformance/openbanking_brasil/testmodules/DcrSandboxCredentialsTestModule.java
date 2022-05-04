package net.openid.conformance.openbanking_brasil.testmodules;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalBrazilDCR;
import net.openid.conformance.fapi1advancedfinal.FAPI1AdvancedFinalBrazilDCRNoSoftwareStatement;
import net.openid.conformance.openbanking_brasil.testmodules.support.AddSupportedOpenIdScopesToClientConfig;
import net.openid.conformance.openbanking_brasil.testmodules.support.CheckScopesFromDynamicRegistrationEndpointDoNotExceedRequestedOpenBankingScopes;
import net.openid.conformance.openbanking_brasil.testmodules.support.ReplaceByHardcodedSandboxCredentials;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "dcr-test-sandbox-credentials",
	displayName = "FAPI1-Advanced-Final: Brazil DCR no software statement",
	summary = "Set hardcoded credentials from Sandbox Environment and perform the DCR flow, but without including a software statement (the values in the software statement are added to the body of the request) - the server must reject the registration attempt. ",
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
public class DcrSandboxCredentialsTestModule extends AbstractFAPI1AdvancedFinalBrazilDCR {

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

	@Override
	protected void callRegistrationEndpoint() {
		callAndStopOnFailure(AddSupportedOpenIdScopesToClientConfig.class);

		callAndStopOnFailure(ReorderGrantTypesInDynamicRegistrationRequest.class, "RFC7591-2");

		callAndStopOnFailure(AddScopeToDynamicRegistrationRequest.class, "RFC7591-2");

		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class, "RFC7591-3.1", "OIDCR-3.2");

		call(exec().mapKey("endpoint_response", "dynamic_registration_endpoint_response"));

		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE,"OIDCR-3.2");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, Condition.ConditionResult.FAILURE,"OIDCR-3.2");
		call(exec().unmapKey("endpoint_response"));

		fireTestFinished();
		eventLog.endBlock();
	}

	@Override
	public  void unregisterClient1(){

	}
}
