package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CallClientConfigurationEndpointAllowingTLSFailure;
import net.openid.conformance.condition.client.CheckNoClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClient;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClientExpectingFailure;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazildcr-client-delete",
	displayName = "FAPI1-Advanced-Final: Brazil DCR client deletion",
	summary = "Obtain a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration), register a new client on the target authorization server then check behaviour of GET/DELETE operations after client deletion.",
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
public class FAPI1AdvancedFinalBrazilDCRClientDeletion extends AbstractFAPI1AdvancedFinalBrazilDCR {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.startBlock("Deleting client then expecting GET / DELETE on configuration endpoint to fail");
		callAndContinueOnFailure(UnregisterDynamicallyRegisteredClient.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2.3");

		callAndStopOnFailure(CallClientConfigurationEndpoint.class, "OIDCD-4.2");

		call(exec().mapKey("endpoint_response", "registration_client_endpoint_response"));

		callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "RFC7592-2.1");
		callAndContinueOnFailure(CheckNoClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE);

		call(exec().unmapKey("endpoint_response"));

		callAndContinueOnFailure(UnregisterDynamicallyRegisteredClientExpectingFailure.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2.3");

		// we already deregistered the client, so prevent cleanup from trying to do so again
		env.removeNativeValue("registration_client_uri");
		env.removeNativeValue("registration_access_token");

		fireTestFinished();
	}

}
