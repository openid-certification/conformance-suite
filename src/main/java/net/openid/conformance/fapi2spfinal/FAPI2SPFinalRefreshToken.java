package net.openid.conformance.fapi2spfinal;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddPromptConsentToAuthorizationEndpointRequestIfScopeContainsOfflineAccess;
import net.openid.conformance.condition.client.AddScopeToTokenEndpointRequest;
import net.openid.conformance.condition.client.CDRRefreshTokenRequiredWhenSharingDurationRequested;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingDpopNonceErrorOrTLSFailureAndReturnFullResponse;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingTLSFailure;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400or401;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedInvalidClientGrantOrRequestError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedInvalidClientGrantRequestOrAttestationError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CreateRefreshTokenRequest;
import net.openid.conformance.condition.client.EnsureRefreshTokenContainsAllowedCharactersOnly;
import net.openid.conformance.condition.client.EnsureServerConfigurationSupportsRefreshToken;
import net.openid.conformance.condition.client.ExpectNoIdTokenInTokenResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractRefreshTokenFromTokenResponse;
import net.openid.conformance.condition.client.FAPIBrazilRefreshTokenRequired;
import net.openid.conformance.condition.client.FAPIEnsureServerConfigurationDoesNotSupportRefreshToken;
import net.openid.conformance.condition.client.ValidateErrorDescriptionFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateErrorUriFromTokenEndpointResponseError;
import net.openid.conformance.condition.client.ValidateRefreshTokenNotRotated;
import net.openid.conformance.condition.client.WaitFor30Seconds;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;
import net.openid.conformance.sequence.client.RefreshTokenRequestExpectingErrorSteps;
import net.openid.conformance.sequence.client.RefreshTokenRequestSteps;
import net.openid.conformance.testmodule.PublishTestModule;
import net.openid.conformance.variant.ClientAuthType;
import net.openid.conformance.variant.FAPI2FinalOPProfile;
import net.openid.conformance.variant.VariantNotApplicable;
import net.openid.conformance.variant.VariantSetup;
import net.openid.conformance.vci10issuer.condition.clientattestation.CreateClientAttestationJwt;
import net.openid.conformance.vci10issuer.condition.clientattestation.GenerateClientAttestationClientInstanceKey;

@PublishTestModule(
	testName = "fapi2-security-profile-final-refresh-token",
	displayName = "FAPI2-Security-Profile-Final: test refresh token behaviours",
	summary = """
		This test obtains refresh tokens and performs various checks, including checking that the refresh token is correctly bound to the client.

		For every (client_auth_type, sender_constrain) combination except client_auth_type=mtls + sender_constrain=mtls (where the missing-proof-of-possession block already exercises this), it verifies that the authorization server rejects a refresh token request that omits client authentication (RFC 6749 §6).

		When client_auth_type=client_attestation, it additionally verifies the OAuth2-ATCA §10.3 binding requirement: the authorization server must reject a refresh token request that uses a different client instance key than the one bound at issuance.
		""",
	profile = "FAPI2-Security-Profile-Final"
)

@VariantNotApplicable(parameter = FAPI2FinalOPProfile.class, values = { "fapi_client_credentials_grant" })

public class FAPI2SPFinalRefreshToken extends AbstractFAPI2SPFinalMultipleClient {

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

	protected void addPromptConsentToAuthorizationEndpointRequest() {
		callAndStopOnFailure(AddPromptConsentToAuthorizationEndpointRequestIfScopeContainsOfflineAccess.class, "OIDCC-11");
	}

	@Override
	protected void createAuthorizationRequest() {
		super.createAuthorizationRequest();
		addPromptConsentToAuthorizationEndpointRequest();
	}

	protected void sendRefreshTokenRequestAndCheckIdTokenClaims() {
		eventLog.startBlock(currentClientString() + "Check for refresh token");
		callAndContinueOnFailure(ExtractRefreshTokenFromTokenResponse.class, Condition.ConditionResult.INFO);

		//stop if no refresh token is returned
		if(Strings.isNullOrEmpty(env.getString("refresh_token"))) {
			if (getVariant(FAPI2FinalOPProfile.class) == FAPI2FinalOPProfile.CONSUMERDATARIGHT_AU) {
				// this will always fail & stop
				callAndStopOnFailure(CDRRefreshTokenRequiredWhenSharingDurationRequested.class, "CDR-requesting-sharing-duration");
			}
			if (getVariant(FAPI2FinalOPProfile.class) == FAPI2FinalOPProfile.OPENBANKING_BRAZIL) {
				// this will always fail & stop
				callAndStopOnFailure(FAPIBrazilRefreshTokenRequired.class, "BrazilOB-5.2.2-11");
			}
			callAndContinueOnFailure(FAPIEnsureServerConfigurationDoesNotSupportRefreshToken.class, Condition.ConditionResult.WARNING, "OIDCD-3");
			// This throws an exception: the test will stop here
			fireTestSkipped("Refresh tokens cannot be tested. No refresh token was issued.");
		}
		callAndContinueOnFailure(EnsureServerConfigurationSupportsRefreshToken.class, Condition.ConditionResult.WARNING, "OIDCD-3");
		callAndContinueOnFailure(EnsureRefreshTokenContainsAllowedCharactersOnly.class, Condition.ConditionResult.FAILURE, "RFC6749-A.17");
		eventLog.endBlock();
		ConditionSequence sequence = new RefreshTokenRequestSteps(isSecondClient(), addClientAuthentication, isDpop());
		if (getVariant(FAPI2FinalOPProfile.class) == FAPI2FinalOPProfile.OPENBANKING_BRAZIL) {
			sequence = sequence.insertAfter(ExtractIdTokenFromTokenResponse.class,
				condition(ValidateRefreshTokenNotRotated.class).requirement("BrazilOB-5.2.2-15").dontStopOnFailure());
		}

		if (! isOpenId) {
			sequence = sequence.insertBefore(ExtractIdTokenFromTokenResponse.class,
				condition(ExpectNoIdTokenInTokenResponse.class));
		}

		// Save the refresh token prior to, possibly, obtaining a new one.
		env.putString("refresh_token_prev", env.getString("refresh_token"));
		call(sequence);
		call(profileBehavior.afterTokenEndpointResponseProcessed());

		if (getVariant(FAPI2FinalOPProfile.class) != FAPI2FinalOPProfile.OPENBANKING_BRAZIL) {
			if (env.getString("refresh_token_prev").equals(env.getString("refresh_token"))) {
				eventLog.log(getName(), "Refresh token not rotated. Skipping lost refresh token test.");
			}
			else {
				// Restore the previous refresh token.
				env.putString("refresh_token", env.getString("refresh_token_prev"));

				ConditionSequence sequence1 = new RefreshTokenRequestSteps(isSecondClient(), addClientAuthentication, isDpop(), "Refresh Token Request With Previous Token, FAPI 2.0 Security Profile 5.3.2.1-9").butFirst(condition(WaitFor30Seconds.class));
				call(sequence1);
				call(profileBehavior.afterTokenEndpointResponseProcessed());
			}
		}

		env.removeNativeValue("refresh_token_prev");

		if (! isSecondClient()) {

			if (getVariant(ClientAuthType.class) == ClientAuthType.CLIENT_ATTESTATION) {
				// OAuth2-ATCA §10.3: the refresh_token is bound to the client instance and its
				// associated public key, not just the client. The client MUST use the same key
				// that was present in the "cnf" claim of the client attestation when the
				// refresh_token was issued.

				// Snapshot the original instance key + attestation so the test can be restored
				// to a working state for any subsequent flow (e.g. second-client phase).
				String origInstanceKey = env.getString("client", "client_instance_key");
				String origInstanceKeyPublic = env.getString("client", "client_instance_key_public");
				String origAttestation = env.getString("client", "client_attestation");

				eventLog.startBlock("Attempting to use the refresh_token with a different client instance key");

				// Generate a fresh instance key and re-mint the client attestation JWT with the
				// new public key in cnf. The resulting refresh request is internally consistent
				// (PoP signed with K2, attestation cnf=K2) but uses K2 ≠ K1, so the AS must
				// reject it. The sender-constraint proof is kept in place so a rejection isolates
				// the §10.3 binding failure mode rather than mixing in missing-PoP.
				callAndStopOnFailure(GenerateClientAttestationClientInstanceKey.class, "OAuth2-ATCA07-10.3");
				callAndStopOnFailure(CreateClientAttestationJwt.class, "OAuth2-ATCA07-10.3");

				callAndStopOnFailure(CreateRefreshTokenRequest.class);
				callAndStopOnFailure(AddScopeToTokenEndpointRequest.class, "RFC6749-6");

				callSenderConstrainedTokenEndpoint("OAuth2-ATCA07-10.3");

				callAndContinueOnFailure(CheckTokenEndpointHttpStatus400or401.class, ConditionResult.FAILURE, "OAuth2-ATCA07-10.3");
				callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, ConditionResult.FAILURE, "OIDCC-3.1.3.4");
				callAndContinueOnFailure(CheckTokenEndpointReturnedInvalidClientGrantRequestOrAttestationError.class, ConditionResult.FAILURE, "OAuth2-ATCA07-10.3", "OAuth2-ATCA07-6.2");

				env.putString("client", "client_instance_key", origInstanceKey);
				env.putString("client", "client_instance_key_public", origInstanceKeyPublic);
				env.putString("client", "client_attestation", origAttestation);

				eventLog.endBlock();
			}

			// RFC 6749 §6 requires confidential clients to authenticate when refreshing an
			// access token. OAuth2-ATCA §10.3 layers on the stronger requirement that the
			// CLIENT_ATTESTATION variant must use the attestation mechanism specifically.
			// Skipped when client_auth_type=mtls AND sender_constrain=mtls: the credentials
			// share the TLS cert, and the no-PoP block below already exercises that combo by
			// unmapping the cert.
			boolean clientAuthMtls = getVariant(ClientAuthType.class) == ClientAuthType.MTLS;
			if (!clientAuthMtls || !isMTLS()) {
				eventLog.startBlock("Attempting to use the refresh_token without client authentication");

				if (clientAuthMtls) {
					// client_auth_type=mtls + sender_constrain=dpop: unmap the bound cert so the
					// AS sees an unauthenticated TLS connection. setupMTLS() switches the token
					// endpoint to the mTLS alias, so the call may fail at TLS handshake — use
					// the TLS-tolerant call mechanism so the dropped connection counts as an
					// acceptable rejection.
					env.mapKey("mutual_tls_authentication", "none_existent_key");
				}

				callAndStopOnFailure(CreateRefreshTokenRequest.class);
				callAndStopOnFailure(AddScopeToTokenEndpointRequest.class, "RFC6749-6");
				// For non-mtls auth, intentionally skip addClientAuthentication — no
				// client_assertion / client_secret / OAuth-Client-Attestation* present.
				if (clientAuthMtls) {
					// mtls auth still adds client_id to the form; the cert is what's broken.
					addClientAuthenticationToTokenEndpointRequest();
				}

				if (clientAuthMtls && isDpop()) {
					// mtls auth + dpop sender: the call may fail at TLS handshake OR the AS may
					// issue a DPoP nonce challenge before checking the cert; tolerate both.
					for (int i = 0; i < 2; i++) {
						createDpopForTokenEndpoint();
						callAndStopOnFailure(CallTokenEndpointAllowingDpopNonceErrorOrTLSFailureAndReturnFullResponse.class, ConditionResult.FAILURE, "FAPI2-SP-FINAL-5.3.2.1-6");
						if (env.getBoolean("token_endpoint_response_ssl_error")) {
							break;
						}
						extractAndValidateClientAttestationChallengeResponseHeader("token_endpoint_response_full");
						if (Strings.isNullOrEmpty(env.getString("token_endpoint_dpop_nonce_error"))) {
							break;
						}
					}
				} else if (isDpop()) {
					// non-mtls auth + dpop sender: retry once if the AS asks for a nonce
					// (mirrors callSenderConstrainedTokenEndpoint).
					for (int i = 0; i < 2; i++) {
						createDpopForTokenEndpoint();
						callAndStopOnFailure(CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse.class);
						extractAndValidateClientAttestationChallengeResponseHeader("token_endpoint_response_full");
						if (Strings.isNullOrEmpty(env.getString("token_endpoint_dpop_nonce_error"))) {
							break;
						}
					}
				} else {
					// non-mtls auth + mtls sender. Cert is intact (only mtls auth unmaps it),
					// so the TLS handshake succeeds; the AS sees the request with a valid cert
					// but no client authentication parameter.
					callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);
					extractAndValidateClientAttestationChallengeResponseHeader("token_endpoint_response_full");
				}

				Boolean sslError = env.getBoolean("token_endpoint_response_ssl_error");
				if (sslError == null || !sslError) {
					callAndContinueOnFailure(CheckTokenEndpointHttpStatus400or401.class, ConditionResult.FAILURE, "RFC6749-6");
					callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, ConditionResult.FAILURE, "OIDCC-3.1.3.4");
					if (getVariant(ClientAuthType.class) == ClientAuthType.CLIENT_ATTESTATION) {
						// OAuth2-ATCA §6.2 lets the AS use `invalid_client_attestation` /
						// `use_fresh_attestation` for an attestation-validation failure.
						callAndContinueOnFailure(CheckTokenEndpointReturnedInvalidClientGrantRequestOrAttestationError.class, ConditionResult.FAILURE, "RFC6749-6", "OAuth2-ATCA07-6.2");
					} else {
						callAndContinueOnFailure(CheckTokenEndpointReturnedInvalidClientGrantOrRequestError.class, ConditionResult.FAILURE, "RFC6749-6");
					}
				}
				// else: TLS handshake was dropped by the server, which is an acceptable way
				// for an mTLS-enforcing AS to indicate the cert is required.

				if (clientAuthMtls) {
					env.unmapKey("mutual_tls_authentication");
				}

				eventLog.endBlock();
			}

			// Ensure a sender constrained refresh_token grant attempt, sent without proof of possession, fails.

			if (isMTLS()) {
				eventLog.startBlock("Attempting to use an MTLS sender constrained refresh_token without proof of possession");
				env.mapKey("mutual_tls_authentication", "none_existent_key");
			}
			else {
				eventLog.startBlock("Attempting to use an DPOP sender constrained refresh_token without proof of possession");
			}

			callAndStopOnFailure(CreateRefreshTokenRequest.class);
			callAndStopOnFailure(AddScopeToTokenEndpointRequest.class, "RFC6749-6");

			mapClientAuthKeys("token_endpoint_request_form_parameters", "token_endpoint_request_headers");
			call(sequence(addClientAuthentication));
			unmapClientAuthKeys();

			if (isMTLS()) {
				callAndStopOnFailure(CallTokenEndpointAllowingTLSFailure.class, ConditionResult.FAILURE,  "FAPI2-SP-FINAL-5.3.2.1-6");
				Boolean sslError = env.getBoolean("token_endpoint_response_ssl_error");
				if (sslError != null && sslError) {
					// the ssl connection was dropped; that's an acceptable way for a server to indicate that a TLS client cert
					// is required, so there's no further checks to do
				} else {
					callAndContinueOnFailure(CheckTokenEndpointHttpStatus400or401.class, ConditionResult.FAILURE, "RFC6749-5.2");

					// this is only a warning to allow for an SSL terminator returning a generic 400 response
					callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, ConditionResult.WARNING, "OIDCC-3.1.3.4");

					if (env.getBoolean(CheckTokenEndpointReturnedJsonContentType.tokenEndpointResponseWasJsonKey)) {
						call(sequence(validateTokenEndpointResponseSteps));
						callAndContinueOnFailure(ValidateErrorFromTokenEndpointResponseError.class, ConditionResult.FAILURE, "RFC6749-5.2");
						callAndContinueOnFailure(CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB.class, ConditionResult.WARNING, "RFC6749-5.2");
						callAndContinueOnFailure(ValidateErrorDescriptionFromTokenEndpointResponseError.class, ConditionResult.FAILURE, "RFC6749-5.2");
						callAndContinueOnFailure(ValidateErrorUriFromTokenEndpointResponseError.class, ConditionResult.FAILURE, "RFC6749-5.2");
					}
				}
			}
			else {
				// call token endpoint without DPOP since this part tests response to ensure error was returned
				// see comment at top of outer block
				callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);

				callAndStopOnFailure(ValidateErrorFromTokenEndpointResponseError.class);
				callAndContinueOnFailure(CheckTokenEndpointHttpStatus400.class, ConditionResult.FAILURE, "OIDCC-3.1.3.4");
				callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, ConditionResult.FAILURE, "OIDCC-3.1.3.4");
				callAndContinueOnFailure(CheckTokenEndpointReturnedInvalidClientGrantOrRequestError.class, ConditionResult.FAILURE, "RFC6749-5.2");
			}

			eventLog.endBlock();

			if (isMTLS()) {
				env.unmapKey("mutual_tls_authentication");
			}
		}
	}

	@Override
	protected void onPostAuthorizationFlowComplete() {
		if (!isSecondClient()) {
			// Try the second client

			//remove refresh token from 1st client
			env.removeNativeValue("refresh_token");

			// Remove token mappings
			// (This must be done before restarting the authorization flow, because
			// handleSuccessfulAuthorizationEndpointResponse extracts an id token)
			env.unmapKey("access_token");
			env.unmapKey("id_token");

			performAuthorizationFlowWithSecondClient();
		} else {
			switchToClient1AndTryClient2AccessToken();

			// try client 2's refresh_token with client 1
			eventLog.startBlock("Attempting to use refresh_token issued to client 2 with client 1");
			call(new RefreshTokenRequestExpectingErrorSteps(isSecondClient(), addClientAuthentication, isDpop()));
			eventLog.endBlock();
			fireTestFinished();
		}
	}

	@Override
	protected void exchangeAuthorizationCode() {
		// Store the original access token and ID token separately (see RefreshTokenRequestSteps)
		env.mapKey("access_token", "first_access_token");
		env.mapKey("id_token", "first_id_token");

		super.exchangeAuthorizationCode();

		// Set up the mappings for the refreshed access and ID tokens
		env.mapKey("access_token", "second_access_token");
		env.mapKey("id_token", "second_id_token");

		sendRefreshTokenRequestAndCheckIdTokenClaims();
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
