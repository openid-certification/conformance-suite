package net.openid.conformance.fapi2spfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingTLSFailure;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400or401;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedInvalidClientGrantOrRequestError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedInvalidRequestGrantOrDPopProofError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.EnsureHttpStatusCodeIs4xx;
import net.openid.conformance.condition.client.ExtractTLSTestValuesFromServerConfiguration;
import net.openid.conformance.condition.client.RemoveMTLSCertificates;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.condition.common.CheckForBCP195InsecureFAPICiphers;
import net.openid.conformance.condition.common.DisallowTLS10;
import net.openid.conformance.condition.common.DisallowTLS11;
import net.openid.conformance.condition.common.EnsureTLS12RequireBCP195Ciphers;
import net.openid.conformance.condition.common.EnsureTLS13OrLater;
import net.openid.conformance.condition.common.EnsureTLS13PreferredOverTLS12;
import net.openid.conformance.condition.common.RequireOnlyBCP195RecommendedCiphersForTLS12;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "fapi2-security-profile-final-ensure-holder-of-key-required",
	displayName = "FAPI2-Security-Profile-Final: ensure holder of key required",
	summary = "This test ensures that all endpoints comply with the TLS version/cipher limitations and that the token endpoint returns an error if a valid request is sent without a holder of key mechanism (i.e. without DPoP / MTLS).",
	profile = "FAPI2-Security-Profile-Final"
)
public class FAPI2SPFinalEnsureHolderOfKeyRequired extends AbstractFAPI2SPFinalServerTestModule {

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

	@VariantSetup(parameter = ClientAuthType.class, value = "client_attestation")
	@Override
	public void setupClientAttestation() {
		super.setupClientAttestation();
		validateTokenEndpointResponseSteps = isDpop() ? ValidateTokenEndpointResponseWithDpop.class : ValidateTokenEndpointResponseWithMTLS.class;
	}

	@Override
	public void start() {
		setStatus(Status.RUNNING);

		callAndStopOnFailure(ExtractTLSTestValuesFromServerConfiguration.class);

		// check that all known endpoints support TLS correctly

		if (! clientCredentialsGrant) {
			eventLog.startBlock("Authorization endpoint TLS test");
			env.mapKey("tls", "authorization_endpoint_tls");
			callAndContinueOnFailure(EnsureTLS12RequireBCP195Ciphers.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.3-2", "FAPI-ISSUES-847");
			callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.1-1,FAPI2-SP-FINAL-5.2.1-3");
			callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.1-1,FAPI2-SP-FINAL-5.2.1-3");
			callAndContinueOnFailure(EnsureTLS13OrLater.class, Condition.ConditionResult.WARNING, "RFC9325-3.1.1");
			call(condition(EnsureTLS13PreferredOverTLS12.class)
				.skipIfStringMissing("tls13_negotiated")
				.onFail(Condition.ConditionResult.FAILURE)
				.requirement("RFC9325-3.1.1")
				.dontStopOnFailure());
			// additional ciphers are allowed on the authorization endpoint

			eventLog.startBlock("Userinfo Endpoint TLS test");
			env.mapKey("tls", "userinfo_endpoint_tls");
			skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, EnsureTLS12RequireBCP195Ciphers.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.3-2", "FAPI-ISSUES-847");
			skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.1-1,FAPI2-SP-FINAL-5.2.1-3");
			skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.1-1,FAPI2-SP-FINAL-5.2.1-3");
			skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, EnsureTLS13OrLater.class, Condition.ConditionResult.WARNING, "RFC9325-3.1.1");
			call(condition(EnsureTLS13PreferredOverTLS12.class)
				.skipIfStringMissing("tls13_negotiated")
				.onFail(Condition.ConditionResult.FAILURE)
				.requirement("RFC9325-3.1.1")
				.dontStopOnFailure());
			skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, RequireOnlyBCP195RecommendedCiphersForTLS12.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.2", "FAPI-ISSUES-847");
		}

		eventLog.startBlock("Token Endpoint TLS test");
		env.mapKey("tls", "token_endpoint_tls");
		callAndContinueOnFailure(EnsureTLS12RequireBCP195Ciphers.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.3-2", "FAPI-ISSUES-847");
		callAndContinueOnFailure(DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.1-1,FAPI2-SP-FINAL-5.2.1-3");
		callAndContinueOnFailure(DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.1-1,FAPI2-SP-FINAL-5.2.1-3");
		callAndContinueOnFailure(EnsureTLS13OrLater.class, Condition.ConditionResult.WARNING, "RFC9325-3.1.1");
		call(condition(EnsureTLS13PreferredOverTLS12.class)
			.skipIfStringMissing("tls13_negotiated")
			.onFail(Condition.ConditionResult.FAILURE)
			.requirement("RFC9325-3.1.1")
			.dontStopOnFailure());
		callAndContinueOnFailure(RequireOnlyBCP195RecommendedCiphersForTLS12.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.2", "FAPI-ISSUES-847");
		callAndContinueOnFailure(CheckForBCP195InsecureFAPICiphers.class, Condition.ConditionResult.WARNING, "FAPI2-SP-FINAL-5.2.2", "RFC9325A-A", "RFC9325-4.2");

		eventLog.startBlock("Registration Endpoint TLS test");
		env.mapKey("tls", "registration_endpoint_tls");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, EnsureTLS12RequireBCP195Ciphers.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.3-2", "FAPI-ISSUES-847");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS10.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.1-1,FAPI2-SP-FINAL-5.2.1-3");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, DisallowTLS11.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.1-1,FAPI2-SP-FINAL-5.2.1-3");
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, EnsureTLS13OrLater.class, Condition.ConditionResult.WARNING, "RFC9325-3.1.1");
		call(condition(EnsureTLS13PreferredOverTLS12.class)
			.skipIfStringMissing("tls13_negotiated")
			.onFail(Condition.ConditionResult.FAILURE)
			.requirement("RFC9325-3.1.1")
			.dontStopOnFailure());
		skipIfMissing(new String[] {"tls"}, null, Condition.ConditionResult.INFO, RequireOnlyBCP195RecommendedCiphersForTLS12.class, Condition.ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.2.2", "FAPI-ISSUES-847");

		eventLog.endBlock();
		env.unmapKey("tls");

		if (clientCredentialsGrant) {
			performCredentialsFlow();
		}
		else {
			performAuthorizationFlow();
		}
	}

	@Override
	protected void handleSuccessfulAuthorizationEndpointResponse() {
		performPostAuthorizationFlow();
	}

	@Override
	protected void performPostAuthorizationFlow() {
		if (clientCredentialsGrant) {
			createClientCredentialsGrantRequest();
		}
		else {
			createAuthorizationCodeRequest();
		}

		// Add client authentication (e.g. client_id for mtls, client_assertion for private_key_jwt) so the
		// server can identify the client, but deliberately omit the holder-of-key / sender-constraining
		// mechanism. For mtls/RFC8705 client auth the client_id parameter is mandatory; without it the server
		// returns invalid_client (it cannot identify the client) rather than the holder-of-key error we expect.
		addClientAuthenticationToTokenEndpointRequest();

		if (isDpop()) {
			// nothing to do; creating the new request above cleared out any previous DPoP header, so the request
			// carries no DPoP proof. That missing holder-of-key mechanism is what this test checks the server rejects.
		} else {
			callAndStopOnFailure(RemoveMTLSCertificates.class);
		}

		callAndStopOnFailure(CallTokenEndpointAllowingTLSFailure.class, Condition.ConditionResult.FAILURE,  "FAPI2-SP-FINAL-5.3.2.1-6");
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
			callAndContinueOnFailure(CheckTokenEndpointReturnedInvalidRequestGrantOrDPopProofError.class, Condition.ConditionResult.FAILURE, "RFC6749-5.2", "RFC9449-5");
		}
	}

}
