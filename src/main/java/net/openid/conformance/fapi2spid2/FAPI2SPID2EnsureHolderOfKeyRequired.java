package net.openid.conformance.fapi2spid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingTLSFailure;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400or401;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedInvalidClientGrantOrRequestError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedInvalidGrantOrRequestError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromServerConfiguration;
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
	testName = "fapi2-security-profile-id2-ensure-holder-of-key-required",
	displayName = "FAPI2-Security-Profile-ID2: ensure holder of key required",
	summary = "This test ensures that all endpoints comply with the TLS version/cipher limitations and that the token endpoint returns an error if a valid request is sent without a holder of key mechanism (i.e. without DPoP / MTLS).",
	profile = "FAPI2-Security-Profile-ID2",
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
public class FAPI2SPID2EnsureHolderOfKeyRequired extends AbstractFAPI2SPID2ServerTestModule {

	private Class<? extends ConditionSequence> validateTokenEndpointResponseSteps;

	@VariantSetup(parameter = ClientAuthType.class, value = "mtls")
	@Override
	public void setupMTLS() {
		super.setupMTLS();
		validateTokenEndpointResponseSteps = isDpop() ? ValidateTokenEndpointResponseWithDpop.class : ValidateTokenEndpointResponseWithMTLS.class;
	}

	@VariantSetup(parameter = ClientAuthType.class, value = "private_key_jwt")
	@Override
	public void setupPrivateKeyJwt() {
		super.setupPrivateKeyJwt();
		validateTokenEndpointResponseSteps = isDpop() ? ValidateTokenEndpointResponseWithDpop.class : ValidateTokenEndpointResponseWithPrivateKeyAndMTLSHolderOfKey.class;
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		callAndStopOnFailure(ExtractTLSTestValuesFromServerConfiguration.class);

		// check that all known endpoints support TLS correctly

		eventLog.startBlock("Authorization endpoint TLS test");
		env.mapKey("tls", "authorization_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.3-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.1-1");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.1-1");
		// additional ciphers are allowed on the authorization endpoint

		eventLog.startBlock("Token Endpoint TLS test");
		env.mapKey("tls", "token_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.3-2");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.1-1");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.1-1");
		callAndContinueOnFailure(DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.2.1");
		callAndContinueOnFailure(CheckForBCP195InsecureFAPICiphers.class, Condition.ConditionResult.WARNING, "FAPI1-ADV-8.5", "RFC9325A-A", "RFC9325-4.2");

		eventLog.startBlock("Userinfo Endpoint TLS test");
		env.mapKey("tls", "userinfo_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.3-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.1-1");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.1-1");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.2.1");

		eventLog.startBlock("Registration Endpoint TLS test");
		env.mapKey("tls", "registration_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, EnsureTLS12WithFAPICiphers.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.3-2");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.1-1");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.1-1");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowInsecureCipher.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-ID2-5.2.2.1");

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

		if (isDpop()) {
			// nothing to do; creating the new request cleared out any previous
			// dpop header
		} else {
			callAndStopOnFailure(RemoveMTLSCertificates.class);
		}

		callAndStopOnFailure(CallTokenEndpointAllowingTLSFailure.class, Condition.ConditionResult.FAILURE,  "FAPI2-SP-ID2-5.3.1.1-6");
		boolean sslError = env.getBoolean("token_endpoint_response_ssl_error");
		if (sslError) {
			// the ssl connection was dropped; that's an acceptable way for a server to indicate that a TLS client cert
			// is required, so there's no further checks to do
		} else {
			call(exec().mapKey("endpoint_response", "token_endpoint_response_full"));
			callAndContinueOnFailure(EnsureHttpStatusCodeIs4xx.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2");
			// these are only warnings to allow for an SSL terminator returning a generic 4xx response due to the missing cert
			callAndContinueOnFailure(CheckTokenEndpointHttpStatus400or401.class, Condition.ConditionResult.WARNING, "RFC6749-5.2");
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

	public static class ValidateTokenEndpointResponseWithDpop extends AbstractConditionSequence {
		@Override
		public void evaluate() {
			// used always when DPoP is the holder of key mechanism
			callAndContinueOnFailure(CheckTokenEndpointReturnedInvalidGrantOrRequestError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2"); //TODO RFC6749?
		}
	}

}
