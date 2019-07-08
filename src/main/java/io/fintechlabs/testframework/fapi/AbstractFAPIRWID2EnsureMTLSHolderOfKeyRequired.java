package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointAllowingTLSFailure;
import io.fintechlabs.testframework.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClient;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointHttpStatus401;
import io.fintechlabs.testframework.condition.client.CheckTokenEndpointReturnedJsonContentType;
import io.fintechlabs.testframework.condition.client.RemoveMTLSCertificates;
import io.fintechlabs.testframework.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateErrorFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import io.fintechlabs.testframework.condition.common.DisallowInsecureCipher;
import io.fintechlabs.testframework.condition.common.DisallowTLS10;
import io.fintechlabs.testframework.condition.common.DisallowTLS11;
import io.fintechlabs.testframework.condition.common.EnsureTLS12;

public abstract class AbstractFAPIRWID2EnsureMTLSHolderOfKeyRequired extends AbstractFAPIRWID2ServerTestModule {

	protected AbstractFAPIRWID2EnsureMTLSHolderOfKeyRequired(StepsConfiguration stepsConfiguration) {
		super(stepsConfiguration);
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		// check that all known endpoints support TLS correctly

		eventLog.startBlock("Authorization endpoint TLS test");
		env.mapKey("tls", "authorization_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		// additional ciphers are allowed on the authorization endpoint

		eventLog.startBlock("Token Endpoint TLS test");
		env.mapKey("tls", "token_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");

		eventLog.startBlock("Userinfo Endpoint TLS test");
		env.mapKey("tls", "userinfo_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, EnsureTLS12.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");

		eventLog.startBlock("Registration Endpoint TLS test");
		env.mapKey("tls", "registration_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, EnsureTLS12.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");

		eventLog.endBlock();
		env.unmapKey("tls");

		performAuthorizationFlow();
	}

	@Override
	protected void performPostAuthorizationFlow() {
		createAuthorizationCodeRequest();

		callAndStopOnFailure(RemoveMTLSCertificates.class);

		callAndContinueOnFailure(CallTokenEndpointAllowingTLSFailure.class, Condition.ConditionResult.FAILURE,  "FAPI-RW-5.2.2-6");
		boolean sslError = env.getBoolean("token_endpoint_response_ssl_error");
		if (sslError) {
			// the ssl connection was dropped; that's an acceptable way for a server to indicate that a TLS client cert
			// is required, so there's no further checks to do
		} else {
			// otherwise we expect a well-formed 'invalid_client' error
			callAndContinueOnFailure(CheckTokenEndpointHttpStatus401.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.3.4");
			callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClient.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		}

		fireTestFinished();
	}

}
