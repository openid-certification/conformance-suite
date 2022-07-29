package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalBrazilDCR;
import net.openid.conformance.openbanking_brasil.testmodules.support.CopyClient;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "dcr-test-multiple-clients",
	displayName = "This test will make sure that the server is allowing multiple clients to be created with the same set of credentials\n" +
		"Test Behaviour:\n" +
		"\u2022 Perform a DCR against the target Server \n" +
		"\u2022 Expect a success 201 - First client_id created for this set of credentials\n" +
		"\u2022 Perform a second DCR against the target Server\n" +
		"\u2022 Expect a failure 400 - The second client_id should not be created for this DCR\n" +
		"\u2022 Unregister all clients → This step should be done if one or both clients have been created",
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

public class DCRMultipleClientTest extends AbstractFAPI1AdvancedFinalBrazilDCR {
	@Override
	protected void setupResourceEndpoint() {
		// Not needed for this test.
	}

	@Override
	protected boolean scopeContains(String requiredScope) {
		return false;
	}

	@Override
	protected void callRegistrationEndpoint() {
		eventLog.startBlock("Create First Client - Expect 200");
		call(sequence(CallDynamicRegistrationEndpointAndVerifySuccessfulResponse.class));
		callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");;
		eventLog.endBlock();

		eventLog.startBlock("Create Second Client - Expect 400");
		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class, "RFC7591-3.1", "OIDCR-3.2");
		call(exec().mapKey("endpoint_response", "dynamic_registration_endpoint_response"));
		callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE, "OIDCR-3.2");
		callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE);
		call(condition(CopyClient.class)
			.skipIfElementMissing("dynamic_registration_endpoint_response", "body_json.client_id"));
		call(condition(ExtractDynamicRegistrationResponse.class)
			.skipIfElementMissing("dynamic_registration_endpoint_response", "body_json.client_id"));
		eventLog.endBlock();
	}

	@Override
	public void cleanup() {
		unregisterClient1();
		unregisterClient2();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);
		fireTestFinished();
	}

	public void unregisterClient2() {
		eventLog.startBlock("Unregister second dynamically registered client");
		env.mapKey("client", "client_copy");
		call(condition(UnregisterDynamicallyRegisteredClient.class)
			.skipIfObjectsMissing("client_copy")
			.onSkip(Condition.ConditionResult.INFO)
			.onFail(Condition.ConditionResult.WARNING)
			.dontStopOnFailure());
		eventLog.endBlock();
	}
}
