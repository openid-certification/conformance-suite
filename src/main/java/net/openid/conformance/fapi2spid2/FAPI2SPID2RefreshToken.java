package net.openid.conformance.fapi2spid2;

import com.google.common.base.Strings;
import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddPromptConsentToAuthorizationEndpointRequestIfScopeContainsOfflineAccess;
import net.openid.conformance.condition.client.AddScopeToTokenEndpointRequest;
import net.openid.conformance.condition.client.CDRRefreshTokenRequiredWhenSharingDurationRequested;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingTLSFailure;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckErrorDescriptionFromTokenEndpointResponseErrorContainsCRLFTAB;
import net.openid.conformance.condition.client.CheckErrorFromTokenEndpointResponseErrorInvalidClientOrInvalidRequest;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus400or401;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatusIs400Allowing401ForInvalidClientError;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedInvalidClientGrantOrRequestError;
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
import net.openid.conformance.variant.FAPI2ID2OPProfile;
import net.openid.conformance.variant.VariantSetup;

@PublishTestModule(
	testName = "fapi2-security-profile-id2-refresh-token",
	displayName = "FAPI2-Security-Profile-ID2: test refresh token behaviours",
	summary = "This tests obtains refresh tokens and performs various checks, including checking that the refresh token is correctly bound to the client.",
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
public class FAPI2SPID2RefreshToken extends AbstractFAPI2SPID2MultipleClient {

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
			if (getVariant(FAPI2ID2OPProfile.class) == FAPI2ID2OPProfile.CONSUMERDATARIGHT_AU) {
				// this will always fail & stop
				callAndStopOnFailure(CDRRefreshTokenRequiredWhenSharingDurationRequested.class, "CDR-requesting-sharing-duration");
			}
			if (getVariant(FAPI2ID2OPProfile.class) == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
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
		ConditionSequence sequence = new RefreshTokenRequestSteps(isSecondClient(), addTokenEndpointClientAuthentication, isDpop());
		if (getVariant(FAPI2ID2OPProfile.class) == FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
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

		if (getVariant(FAPI2ID2OPProfile.class) != FAPI2ID2OPProfile.OPENBANKING_BRAZIL) {
			if (env.getString("refresh_token_prev").equals(env.getString("refresh_token"))) {
				eventLog.log(getName(), "Refresh token not rotated. Skipping lost refresh token test.");
			}
			else {
				// Restore the previous refresh token.
				env.putString("refresh_token", env.getString("refresh_token_prev"));

				ConditionSequence sequence1 = new RefreshTokenRequestSteps(isSecondClient(), addTokenEndpointClientAuthentication, isDpop(), "Refresh Token Request With Previous Token, FAPI 2.0 Security Profile 5.3.1.1-9").butFirst(condition(WaitFor30Seconds.class));
				call(sequence1);
			}
		}

		env.removeNativeValue("refresh_token_prev");

		if (! isSecondClient()) {
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

			call(sequence(addTokenEndpointClientAuthentication));

			if (isMTLS()) {
				callAndStopOnFailure(CallTokenEndpointAllowingTLSFailure.class, ConditionResult.FAILURE,  "FAPI2-SP-ID2-5.3.1.1-6");
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
			call(new RefreshTokenRequestExpectingErrorSteps(isSecondClient(), addTokenEndpointClientAuthentication, isDpop()));
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
