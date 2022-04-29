package net.openid.conformance.sequence.client;

import net.openid.conformance.condition.Condition.ConditionResult;
import net.openid.conformance.condition.client.AddDpopHeaderForTokenEndpointRequest;
import net.openid.conformance.condition.client.AddScopeToTokenEndpointRequest;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;
import net.openid.conformance.condition.client.CheckIfTokenEndpointResponseError;
import net.openid.conformance.condition.client.CheckTokenEndpointCacheHeaders;
import net.openid.conformance.condition.client.CheckTokenEndpointHttpStatus200;
import net.openid.conformance.condition.client.CheckTokenEndpointReturnedJsonContentType;
import net.openid.conformance.condition.client.CheckTokenTypeIsBearer;
import net.openid.conformance.condition.client.CheckTokenTypeIsDpop;
import net.openid.conformance.condition.client.CompareIdTokenClaims;
import net.openid.conformance.condition.client.CreateDpopClaims;
import net.openid.conformance.condition.client.CreateDpopHeader;
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
import net.openid.conformance.condition.client.SetDpopHtmHtuForTokenEndpoint;
import net.openid.conformance.condition.client.SignDpopProof;
import net.openid.conformance.condition.client.ValidateExpiresIn;
import net.openid.conformance.condition.client.WaitForOneSecond;
import net.openid.conformance.sequence.AbstractConditionSequence;
import net.openid.conformance.sequence.ConditionSequence;

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
	private String currentClient;
	private Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest;

	public RefreshTokenRequestSteps(boolean secondClient, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest) {
		this(secondClient, addClientAuthenticationToTokenEndpointRequest, false);
	}

	public RefreshTokenRequestSteps(boolean secondClient, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest, boolean isDpop) {
		this.secondClient = secondClient;
		this.isDpop = isDpop;
		this.currentClient = secondClient ? "Second client: " : "";
		this.addClientAuthenticationToTokenEndpointRequest = addClientAuthenticationToTokenEndpointRequest;
	}

	@Override
	public void evaluate() {
		call(exec().startBlock(currentClient + "Refresh Token Request"));
		callAndStopOnFailure(CreateRefreshTokenRequest.class);
		if (!secondClient) {
			callAndStopOnFailure(AddScopeToTokenEndpointRequest.class, "RFC6749-6");
		}

		if (addClientAuthenticationToTokenEndpointRequest != null) {
			call(sequence(addClientAuthenticationToTokenEndpointRequest));
		}

		//wait 1 second to make sure that iat values will be different
		callAndStopOnFailure(WaitForOneSecond.class);

		if (isDpop) {
			// we generate a new key here, to check the server handles that correctly - so this isn't suitable for
			// public clients where the refresh token is bound to the dpop key
			callAndStopOnFailure(GenerateDpopKey.class);
			callAndStopOnFailure(CreateDpopHeader.class);
			callAndStopOnFailure(CreateDpopClaims.class);
			callAndStopOnFailure(SetDpopHtmHtuForTokenEndpoint.class);
			callAndStopOnFailure(SignDpopProof.class);
			callAndStopOnFailure(AddDpopHeaderForTokenEndpointRequest.class);
		}

		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);
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
		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class, "RFC6749-6", "RFC6749-5.1");
		call(condition(ValidateExpiresIn.class)
				.skipIfObjectMissing("expires_in")
				.requirement("RFC6749-5.1")
				.dontStopOnFailure());

		callAndContinueOnFailure(EnsureAccessTokenValuesAreDifferent.class);

		callAndContinueOnFailure(ExtractIdTokenFromTokenResponse.class);

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
}
