package net.openid.conformance.fapiciba;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingTLSFailure;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400or401;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedInvalidClientGrantOrRequestError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.RemoveMTLSCertificates;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.condition.common.CheckForBCP195InsecureFAPICiphers;
import net.openid.conformance.condition.common.DisallowInsecureCipher;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12WithFAPICiphers;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "fapi-rw-id2-ensure-mtls-holder-of-key-required",
	displayName = "FAPI-RW-ID2: ensure mtls holder of key required",
	summary = "This test ensures that all endpoints comply with the TLS version/cipher limitations and that the token endpoint returns an error if a valid request is sent without a TLS certificate.",
	profile = "FAPI-RW-ID2",
	configurationFields = {
		"server.discoveryUrl",
		"client.client_id",
		"client.scope",
		"client.jwks",
		"mtls.key",
		"mtls.cert",
		"mtls.ca",
		"client2.client_id",
		"client2.scope",
		"client2.jwks",
		"mtls2.key",
		"mtls2.cert",
		"mtls2.ca",
		"resource.resourceUrl"
	}
)
public class FAPIRWID2EnsureMTLSHolderOfKeyRequired extends AbstractFAPIRWID2ServerTestModule {

	private Class<? extends ConditionSequence> validateTokenEndpointResponseSteps;

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	@Override
	public void setupMTLS() {
		super.setupMTLS();
		validateTokenEndpointResponseSteps = ValidateTokenEndpointResponseWithMTLS.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	@Override
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
		validateTokenEndpointResponseSteps = ValidateTokenEndpointResponseWithPrivateKeyAndMTLSHolderOfKey.class;
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		// check that all known endpoints support TLS correctly

		eventLog.startBlock("Authorization endpoint TLS test");
		env.mapKey("tls", "authorization_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		// additional ciphers are allowed on the authorization endpoint

		eventLog.startBlock("Token Endpoint TLS test");
		env.mapKey("tls", "token_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		callAndContinueOnFailure(CheckForBCP195InsecureFAPICiphers.class, Condition.ConditionResult.WARNING, "FAPI1-ADV-8.5", "RFC9325A-A", "RFC9325-4.2");

		eventLog.startBlock("Userinfo Endpoint TLS test");
		env.mapKey("tls", "userinfo_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");

		eventLog.startBlock("Registration Endpoint TLS test");
		env.mapKey("tls", "registration_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI-RW-8.5-2");

		eventLog.endBlock();
		env.unmapKey("tls");

		performAuthorizationFlow();
	}

	@Override
	protected void handleSuccessfulAuthorizationEndpointResponse() {
		performPostAuthorizationFlow();
	}

	@Override
	protected void performPostAuthorizationFlow() {
		createAuthorizationCodeRequest();

		callAndStopOnFailure(RemoveMTLSCertificates.class);

		callAndStopOnFailure(CallTokenEndpointAllowingTLSFailure.class, Condition.ConditionResult.FAILURE,  "FAPI-RW-5.2.2-6");
		boolean sslError = env.getBoolean("token_endpoint_response_ssl_error");
		if (sslError) {
			// the ssl connection was dropped; that's an acceptable way for a server to indicate that a TLS client cert
			// is required, so there's no further checks to do
		} else {
			callAndContinueOnFailure(CheckTokenEndpointHttpStatus400or401.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");

			// this is only a warning to allow for an SSL terminator returning a generic 400 response
			callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, Condition.ConditionResult.WARNING, "OIDCC-3.1.3.4");

			if (env.getBoolean(CheckTokenEndpointReturnedJsonContentType.tokenEndpointResponseWasJsonKey)) {
				call(sequence(validateTokenEndpointResponseSteps));
				callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
				callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
				callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
				callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			}
		}

		fireTestFinished();
	}

	public static class ValidateTokenEndpointResponseWithMTLS extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			// if the SSL connection was not dropped, we expect a well-formed 'invalid_client' error
			callAndContinueOnFailure(CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			callAndContinueOnFailure(CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		}
	}

	public static class ValidateTokenEndpointResponseWithPrivateKeyAndMTLSHolderOfKey extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			// if the ssl connection was not dropped, we expect one of invalid_request, invalid_grant or invalid_client
			callAndContinueOnFailure(CheckTokenEndpointReturnedInvalidClientGrantOrRequestError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
		}
	}
}
