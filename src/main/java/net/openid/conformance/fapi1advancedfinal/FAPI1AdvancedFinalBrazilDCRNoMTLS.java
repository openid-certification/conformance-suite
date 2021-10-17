package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CallClientConfigurationEndpointAllowingTLSFailure;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpointAllowingTLSFailure;
import net.openid.conformance.condition.client.CheckDynamicRegistrationEndpointReturnedError;
import net.openid.conformance.condition.client.CheckNoClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentType;
import net.openid.conformance.condition.client.CheckRegistrationClientEndpointContentTypeHttpStatus200;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs401;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClient;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClientExpectingFailure;
import net.openid.conformance.testmodule.PublishTestModule;

@PublishTestModule(
	testName = "fapi1-advanced-final-brazil-dcr-no-mtls",
	displayName = "FAPI1-Advanced-Final: Brazil DCR no MTLS",
	summary = "Perform the DCR flow, but without presenting a TLS client certificate - the server must reject the registration attempt, either by refusing the TLS negotiation or returning a valid error response.",
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
public class FAPI1AdvancedFinalBrazilDCRNoMTLS extends AbstractFAPI1AdvancedFinalBrazilDCR {

	@Override
	protected void setupResourceEndpoint() {
		// not needed as resource endpoint won't be called
	}

	@Override
	protected void callRegistrationEndpoint() {
		env.mapKey("mutual_tls_authentication", "none_existent_key");

		callAndStopOnFailure(CallDynamicRegistrationEndpointAllowingTLSFailure.class);

		boolean sslError = env.getBoolean(CallDynamicRegistrationEndpointAllowingTLSFailure.RESPONSE_SSL_ERROR_KEY);
		if (sslError) {
			// the ssl connection was dropped; that's an acceptable way for a server to indicate that a TLS client cert
			// is required, so there's no further checks to do
		} else {
			env.mapKey("endpoint_response", "dynamic_registration_endpoint_response");
			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(EnsureHttpStatusCodeIs400.class, Condition.ConditionResult.FAILURE);
			// an error to be returned in this case doesn't really seem to be defined anywhere, so allow any error
			callAndContinueOnFailure(CheckDynamicRegistrationEndpointReturnedError.class, Condition.ConditionResult.FAILURE);
		}

		env.unmapKey("mutual_tls_authentication");

		super.callRegistrationEndpoint();
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		eventLog.startBlock("Call client configuration endpoint with no certificate");
		env.mapKey("mutual_tls_authentication", "none_existent_key");

		callAndStopOnFailure(CallClientConfigurationEndpoint.class, "OIDCD-4.2");
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentTypeHttpStatus200.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");
		callAndContinueOnFailure(CheckRegistrationClientEndpointContentType.class, Condition.ConditionResult.FAILURE, "OIDCD-4.3");

		callAndContinueOnFailure(UnregisterDynamicallyRegisteredClient.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2.3");

		// we just deregistered the client, so prevent cleanup from trying to do so again
		env.removeNativeValue("registration_client_uri");
		env.removeNativeValue("registration_access_token");

		super.onPostAuthorizationFlowComplete();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.startBlock("Call client configuration endpoint with no certificate");
		env.mapKey("mutual_tls_authentication", "none_existent_key");

		callAndStopOnFailure(CallClientConfigurationEndpointAllowingTLSFailure.class, "OIDCD-4.2");
		boolean sslError = env.getBoolean(CallClientConfigurationEndpointAllowingTLSFailure.RESPONSE_SSL_ERROR_KEY);
		if (sslError) {
			// the ssl connection was dropped; that's an acceptable way for a server to indicate that a TLS client cert
			// is required, so there's no further checks to do
		} else {
			call(exec().mapKey("endpoint_response", "registration_client_endpoint_response"));

			callAndContinueOnFailure(EnsureHttpStatusCodeIs401.class, Condition.ConditionResult.FAILURE);
			callAndContinueOnFailure(CheckNoClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE);

			call(exec().unmapKey("endpoint_response"));
		}


		callAndContinueOnFailure(UnregisterDynamicallyRegisteredClientExpectingFailure.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2.3");

		env.unmapKey("mutual_tls_authentication");

		callAndContinueOnFailure(UnregisterDynamicallyRegisteredClient.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2.3");

		// we just deregistered the client, so prevent cleanup from trying to do so again
		env.removeNativeValue("registration_client_uri");
		env.removeNativeValue("registration_access_token");

		fireTestFinished();
	}
}
