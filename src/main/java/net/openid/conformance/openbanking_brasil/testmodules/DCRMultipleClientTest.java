package net.openid.conformance.openbanking_brasil.testmodules;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.*;
import net.openid.conformance.fapi1advancedfinal.AbstractFAPI1AdvancedFinalBrazilDCR;
import net.openid.conformance.openbanking_brasil.testmodules.support.ChuckWarning;
import net.openid.conformance.openbanking_brasil.testmodules.support.CopyClient;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureEndpointResponseWas201;
import net.openid.conformance.openbanking_brasil.testmodules.support.EnsureResponseCodeWas200;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.CallDynamicRegistrationEndpointAndVerifySuccessfulResponse;
import net.openid.conformance.testmodule.PublishTestModule;
import org.apache.http.HttpStatus;

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
		callAndContinueOnFailure(ClientManagementEndpointAndAccessTokenRequired.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2");
		eventLog.endBlock();

		eventLog.startBlock("Create Second Client - Expect 400 - Return warning on 201");
		callAndStopOnFailure(CallDynamicRegistrationEndpoint.class, "RFC7591-3.1", "OIDCR-3.2");
		call(exec().mapKey("endpoint_response", "dynamic_registration_endpoint_response"));

		call(validationSequence());

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

	ConditionSequence validationSequence() {
		ConditionSequence validationSteps = sequenceOf(condition(EnsureContentTypeJson.class),
			condition(EnsureHttpStatusCodeIs400.class));

		int statusCode = env.getInteger("endpoint_response", "status");
		if (statusCode != HttpStatus.SC_BAD_REQUEST) {

			env.putString("warning_message", "The current Open Banking Brazil specification requires servers to support " +
				"only one active client for each software statement. As the decision to mandate this behavior has been " +
				"set on july 2022 this behavior will still be accepted for a few months before becoming mandatory " +
				"- Moment where test will return a failure");
			validationSteps.replace(EnsureHttpStatusCodeIs400.class,condition(EnsureEndpointResponseWas201.class))
				.then(condition(ChuckWarning.class).dontStopOnFailure()
					.onFail(Condition.ConditionResult.WARNING));
		}
		return validationSteps;
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
