package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallClientConfigurationEndpointAllowingTLSFailure;
import net.openid.conformance.condition.client.CallDynamicRegistrationEndpointAllowingTLSFailure;
import net.openid.conformance.condition.client.CheckDynamicRegistrationEndpointReturnedError;
import net.openid.conformance.condition.client.CheckNoClientIdFromClientConfigurationEndpoint;
import net.openid.conformance.condition.client.CheckNoClientIdFromDynamicRegistrationEndpoint;
import net.openid.conformance.condition.client.EnsureContentTypeJson;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs400or401;
import net.openid.conformance.condition.client.UnregisterDynamicallyRegisteredClientExpectingFailure;

public abstract class AbstractFAPI1AdvancedFinalBrazilDCRMTLSIssue extends AbstractFAPI1AdvancedFinalBrazilDCR {
	protected abstract void mapToWrongMTLS();

	@Override
	protected void setupResourceEndpoint() {
		// not needed as resource endpoint won't be called
	}

	@Override
	protected void callRegistrationEndpoint() {
		mapToWrongMTLS();

		eventLog.startBlock("Call dynamic client registration endpoint with no/bad certificate");

		callAndStopOnFailure(CallDynamicRegistrationEndpointAllowingTLSFailure.class);

		boolean sslError = env.getBoolean(CallDynamicRegistrationEndpointAllowingTLSFailure.RESPONSE_SSL_ERROR_KEY);
		if (sslError) {
			// the ssl connection was dropped; that's an acceptable way for a server to indicate that a TLS client cert
			// is required, so there's no further checks to do
		} else {
			env.mapKey("endpoint_response", "dynamic_registration_endpoint_response");
			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.WARNING, "RFC7591-3.2.2");
			callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
			if (env.getBoolean(EnsureContentTypeJson.endpointResponseWasJsonKey)) {
				// an error to be returned in this case doesn't really seem to be defined anywhere, so allow any error
				callAndContinueOnFailure(CheckDynamicRegistrationEndpointReturnedError.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
				callAndContinueOnFailure(CheckNoClientIdFromDynamicRegistrationEndpoint.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
			}
		}

		env.unmapKey("mutual_tls_authentication");

		eventLog.startBlock("Call dynamic client registration endpoint with correct certificate");

		super.callRegistrationEndpoint();
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		eventLog.startBlock("Call client configuration endpoint with no/bad certificate");
		mapToWrongMTLS();

		callAndStopOnFailure(CallClientConfigurationEndpointAllowingTLSFailure.class, "OIDCD-4.2");
		boolean sslError = env.getBoolean(CallClientConfigurationEndpointAllowingTLSFailure.RESPONSE_SSL_ERROR_KEY);
		if (sslError) {
			// the ssl connection was dropped; that's an acceptable way for a server to indicate that a TLS client cert
			// is required, so there's no further checks to do
		} else {
			call(exec().mapKey("endpoint_response", "registration_client_endpoint_response"));

			callAndContinueOnFailure(EnsureHttpStatusCodeIs400or401.class, Condition.ConditionResult.FAILURE, "RFC7592-2.1");
			callAndContinueOnFailure(EnsureContentTypeJson.class, Condition.ConditionResult.WARNING, "RFC7591-3.2.2");
			if (env.getBoolean(EnsureContentTypeJson.endpointResponseWasJsonKey)) {
				callAndContinueOnFailure(CheckNoClientIdFromClientConfigurationEndpoint.class, Condition.ConditionResult.FAILURE);
				env.mapKey("dynamic_registration_endpoint_response", "registration_client_endpoint_response");
				callAndContinueOnFailure(CheckDynamicRegistrationEndpointReturnedError.class, Condition.ConditionResult.FAILURE, "RFC7591-3.2.2");
				call(exec().unmapKey("endpoint_response"));

			}

			call(exec().unmapKey("endpoint_response"));
		}


		callAndContinueOnFailure(UnregisterDynamicallyRegisteredClientExpectingFailure.class, Condition.ConditionResult.FAILURE, "BrazilOBDCR-7.1", "RFC7592-2.3");

		env.unmapKey("mutual_tls_authentication");

		eventLog.startBlock("Deregister client");

		deleteClient();

		fireTestFinished();
	}
}
