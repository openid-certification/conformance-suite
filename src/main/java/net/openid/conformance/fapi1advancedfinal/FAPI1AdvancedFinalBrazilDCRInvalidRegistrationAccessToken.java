package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckNoClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClientExpectingFailure;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazildcr-invalid-registration-access-token",
	displayName = "FAPI1-Advanced-Final: Brazil DCR invalid registration access token",
	summary = "Obtain a software statement from the Brazil directory (using the client MTLS certificate and directory client id provided in the test configuration), register a new client on the target authorization server then check behaviour of GET/DELETE operations when a bad access token.",
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
public class FAPI1AdvancedFinalBrazilDCRInvalidRegistrationAccessToken extends AbstractFAPI1AdvancedFinalBrazilDCR {

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		String accessToken = env.getString("client", "registration_access_token");
		env.putString("client", "registration_access_token", "ivegotthisterriblepaininallthediodesdownmyleftside");

		eventLog.startBlock("Calling GET / DELETE on configuration endpoint with invalid access token");

		callAndStopOnFailure(CallClientConfigurationEndpoint.class, "OIDCD-4.2");

		call(exec().mapKey("endpoint_response", "registration_client_endpoint_response"));

		callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE, "RFC7592-2.1");
		callAndContinueOnFailure(CheckNoClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE);

		call(exec().unmapKey("endpoint_response"));

		callAndContinueOnFailure(UnregisterDynamicallyRegisteredClientExpectingFailure.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2.3");

		env.putString("client", "registration_access_token", accessToken);

		deleteClient();

		fireTestFinished();
	}

}
