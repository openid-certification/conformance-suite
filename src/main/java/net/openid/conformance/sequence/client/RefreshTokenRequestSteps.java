package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddScopeToTokenEndpointRequest;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse;
import net.openid.conformance.condition.client.CallTokenEndpointAllowingUseAttestationChallengeErrorAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.ExtractClientAttestationChallengeFromResponseHeader;
import net.openid.conformance.condition.client.ValidateClientAttestationChallengeResponseHeader;
import net.openid.conformance.condition.client.CheckTokenEndpointCacheHeaders;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus200;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CheckTokenTypeIsBearer;
import net.openid.conformance.condition.client.CheckTokenTypeIsDpop;
import net.openid.conformance.condition.client.CompareIdTokenClaims;
import net.openid.conformance.condition.client.CreateRefreshTokenRequest;
import net.openid.conformance.condition.client.EnsureAccessTokenContainsAllowedCharactersOnly;
import net.openid.conformance.condition.client.EnsureAccessTokenValuesAreDifferent;
import net.openid.conformance.condition.client.EnsureMinimumAccessTokenEntropy;
import net.openid.conformance.condition.client.EnsureMinimumRefreshTokenEntropy;
import net.openid.conformance.condition.client.EnsureMinimumRefreshTokenLength;
import net.openid.conformance.condition.client.ExtractAccessTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import net.openid.conformance.condition.client.ExtractIdTokenFromTokenResponse;
import net.openid.conformance.condition.client.ExtractRefreshTokenFromTokenResponse;
import net.openid.conformance.condition.client.GenerateDpopKey;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.client.ValidateIdTokenFromTokenResponseEncryption;
import net.openid.conformance.condition.client.WaitForOneSecond;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Use the refresh token to fetch a new access token and (possibly) ID token, and compare the two.
 * The original access token and ID token should be stored as "first_access_token" and
 * "first_id_token" respectively, and there should be an environment mapping from "access_token" to
 * "second_access_token", and from "id_token" to "second_id_token".
 * See FAPIRWID2RefreshToken for an example of how to do this.
 */
public class RefreshTokenRequestSteps extends AbstractConditionSequence {

	private boolean secondClient;
	private boolean isDpop;
	private String testTitle;
	private String currentClient;
	private Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest;

	public RefreshTokenRequestSteps(boolean secondClient, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest) {
		this(secondClient, addClientAuthenticationToTokenEndpointRequest, false, null);
	}

	public RefreshTokenRequestSteps(boolean secondClient, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest, boolean isDpop) {
		this(secondClient, addClientAuthenticationToTokenEndpointRequest, isDpop, null);
	}

	public RefreshTokenRequestSteps(boolean secondClient, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest, boolean isDpop, String testTitle) {
		this.secondClient = secondClient;
		this.isDpop = isDpop;
		this.testTitle = testTitle;
		this.currentClient = secondClient ? "Second client: " : "";
		this.addClientAuthenticationToTokenEndpointRequest = addClientAuthenticationToTokenEndpointRequest;
	}

	@Override
	public void evaluate() {
		if (testTitle == null) {
			testTitle = "Refresh Token Request";
		}
		call(exec().startBlock(currentClient + testTitle));

		callAndStopOnFailure(CreateRefreshTokenRequest.class);
		if (!secondClient) {
			callAndStopOnFailure(AddScopeToTokenEndpointRequest.class, "RFC6749-6");
		}

		if (addClientAuthenticationToTokenEndpointRequest != null) {
			call(exec().mapKey("request_form_parameters", "token_endpoint_request_form_parameters")
				.mapKey("request_headers", "token_endpoint_request_headers"));
			call(sequence(addClientAuthenticationToTokenEndpointRequest));
			call(exec().unmapKey("request_form_parameters").unmapKey("request_headers"));
		}

		//wait 1 second to make sure that iat values will be different
		callAndStopOnFailure(WaitForOneSecond.class);

		if (isDpop) {
			// we generate a new key here, to check the server handles that correctly - so this isn't suitable for
			// public clients where the refresh token is bound to the dpop key
			callAndStopOnFailure(GenerateDpopKey.class);
			call(CreateDpopProofSteps.createTokenEndpointDpopSteps());
			callAndStopOnFailure(CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse.class);
			harvestClientAttestationChallengeResponseHeader();

			// retry request if token_endpoint_dpop_nonce_error is found
			call(exec().startBlock("Token endpoint DPoP nonce retry"));

			// repeat conditions in CreateDpopProofSteps.createTokenEndpointDpopSteps() only if token_endpoint_dpop_nonce_error is found
			ConditionSequence seq = CreateDpopProofSteps.createTokenEndpointDpopSteps();
			seq.evaluate();
			List<Class<?extends Condition>> condList = seq.getTestExecutionUnits().stream().map(actionToConditionClass).collect(Collectors.toList());
			condList.forEach((Class<?extends Condition> cond) -> {
				call(condition(cond)
					.skipIfStringsMissing("token_endpoint_dpop_nonce_error")
					.onSkip(ConditionResult.INFO));
			});

			call(condition(CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse.class)
				.skipIfStringsMissing("token_endpoint_dpop_nonce_error")
				.onSkip(ConditionResult.INFO));
			call(exec().endBlock());

			// retry request if token_endpoint_use_attestation_challenge_error is found
			// (draft-ietf-oauth-attestation-based-client-auth-07 §6.2). Re-running the auth sequence
			// regenerates the client_attestation PoP using the just-harvested OAuth-Client-Attestation-Challenge.
			retryOnUseAttestationChallengeError(CallTokenEndpointAllowingDpopNonceErrorAndReturnFullResponse.class);

		} else {
			callAndStopOnFailure(CallTokenEndpointAllowingUseAttestationChallengeErrorAndReturnFullResponse.class);
			harvestClientAttestationChallengeResponseHeader();
			retryOnUseAttestationChallengeError(CallTokenEndpointAllowingUseAttestationChallengeErrorAndReturnFullResponse.class);
		}

		callAndContinueOnFailure(CheckTokenEndpointHttpStatus200.class, ConditionResult.FAILURE, "RFC6749-5.1");
		callAndContinueOnFailure(CheckTokenEndpointReturnedJsonContentType.class, ConditionResult.FAILURE, "RFC6749-5.1");
		callAndContinueOnFailure(CheckTokenEndpointCacheHeaders.class, ConditionResult.FAILURE,  "RFC6749-5.1");
		callAndStopOnFailure(CheckIfTokenEndpointResponseError.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		if (isDpop) {
			callAndContinueOnFailure(CheckTokenTypeIsDpop.class, ConditionResult.FAILURE, "DPOP-5");
		} else {
			callAndContinueOnFailure(CheckTokenTypeIsBearer.class, ConditionResult.FAILURE, "FAPI-R-6.2.2-1", "FAPI1-BASE-6.2.2-1");
		}
		callAndContinueOnFailure(EnsureMinimumAccessTokenEntropy.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-16", "FAPI1-BASE-5.2.2-16");
		callAndContinueOnFailure(EnsureAccessTokenContainsAllowedCharactersOnly.class, ConditionResult.FAILURE, "RFC6749-A.12");
		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, ConditionResult.WARNING, "RFC6749-6", "RFC6749-5.1");
		call(condition(ValidateExpiresIn.class)
				.skipIfObjectMissing("expires_in")
				.requirement("RFC6749-5.1")
				.dontStopOnFailure());

		callAndContinueOnFailure(EnsureAccessTokenValuesAreDifferent.class, ConditionResult.INFO);

		call(condition(ValidateIdTokenFromTokenResponseEncryption.class)
			.skipIfObjectMissing("client_jwks")
			.onSkip(ConditionResult.INFO)
			.onFail(ConditionResult.INFO)
			.dontStopOnFailure());
		callAndContinueOnFailure(ExtractIdTokenFromTokenResponse.class, ConditionResult.INFO);

		// It's perfectly legal to NOT return a new refresh token; if the server didn't then
		// 'refresh_token' in the environment will be left containing the old (still valid)
		// token. We use that token later to test the refresh token is bound to the client
		// correctly.
		callAndContinueOnFailure(ExtractRefreshTokenFromTokenResponse.class, ConditionResult.INFO);

		call(condition(EnsureMinimumRefreshTokenLength.class)
			.skipIfElementMissing("token_endpoint_response", "refresh_token")
			.requirement("RFC6749-10.10")
			.dontStopOnFailure());

		call(condition(EnsureMinimumRefreshTokenEntropy.class)
			.skipIfElementMissing("token_endpoint_response", "refresh_token")
			.requirement("RFC6749-10.10")
			.dontStopOnFailure());

		//compare only when refresh response contains an id_token
		call(condition(CompareIdTokenClaims.class)
				.skipIfObjectMissing("second_id_token")
				.requirement("OIDCC-12.2")
				.dontStopOnFailure());

		call(exec().endBlock());
	}

	/**
	 * Harvest the {@code OAuth-Client-Attestation-Challenge} response header (if any) from the most recent
	 * token endpoint response into {@code vci.attestation_challenge} so the next client-attestation PoP
	 * uses the freshest server-supplied challenge (draft-ietf-oauth-attestation-based-client-auth-07 §8.1).
	 * Safe no-op when the header is absent or another client auth type is in use.
	 */
	private void harvestClientAttestationChallengeResponseHeader() {
		call(exec().mapKey("endpoint_response", "token_endpoint_response_full"));
		callAndContinueOnFailure(ExtractClientAttestationChallengeFromResponseHeader.class, ConditionResult.FAILURE, "OAuth2-ATCA07-8.1");
		callAndContinueOnFailure(ValidateClientAttestationChallengeResponseHeader.class, ConditionResult.WARNING, "OAuth2-ATCA07-8.1");
		call(exec().unmapKey("endpoint_response"));
	}

	/**
	 * Retry the token endpoint request if the AS returned 400 {@code use_attestation_challenge}
	 * (draft-ietf-oauth-attestation-based-client-auth-07 §6.2). All steps are gated on
	 * {@code token_endpoint_use_attestation_challenge_error} being set, so this is a no-op when the
	 * previous response didn't ask for a retry.
	 *
	 * <p>The auth sequence is re-run inside the block so {@link
	 * net.openid.conformance.condition.client.CreateClientAttestationProofJwt} regenerates the PoP
	 * with the just-harvested challenge.
	 */
	private void retryOnUseAttestationChallengeError(Class<? extends Condition> callTokenEndpointClass) {
		call(exec().startBlock("Token endpoint use_attestation_challenge retry"));

		if (addClientAuthenticationToTokenEndpointRequest != null) {
			ConditionSequence authSeq;
			try {
				authSeq = addClientAuthenticationToTokenEndpointRequest.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("Couldn't instantiate client authentication sequence "
					+ addClientAuthenticationToTokenEndpointRequest.getSimpleName(), e);
			}
			authSeq.evaluate();
			List<Class<? extends Condition>> authConds = authSeq.getTestExecutionUnits().stream()
				.map(actionToConditionClass)
				.filter(java.util.Objects::nonNull)
				.collect(Collectors.toList());

			call(exec().mapKey("request_form_parameters", "token_endpoint_request_form_parameters")
				.mapKey("request_headers", "token_endpoint_request_headers"));
			authConds.forEach(cond -> call(condition(cond)
				.skipIfStringsMissing("token_endpoint_use_attestation_challenge_error")
				.onSkip(ConditionResult.INFO)));
			call(exec().unmapKey("request_form_parameters").unmapKey("request_headers"));
		}

		call(condition(callTokenEndpointClass)
			.skipIfStringsMissing("token_endpoint_use_attestation_challenge_error")
			.onSkip(ConditionResult.INFO));

		call(exec().endBlock());

		// Harvest the latest response's OAuth-Client-Attestation-Challenge header. Run
		// unconditionally because the retry call clears
		// token_endpoint_use_attestation_challenge_error at the start of its evaluate(),
		// so a gated harvest would miss a fresh challenge returned in a successful retry
		// response. If the retry block didn't run, this is an idempotent re-harvest of
		// the original response.
		harvestClientAttestationChallengeResponseHeader();
	}
}
