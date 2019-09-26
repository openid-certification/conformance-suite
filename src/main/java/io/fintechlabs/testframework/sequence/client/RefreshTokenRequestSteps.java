package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddScopeToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointAndReturnFullResponse;
import io.fintechlabs.testframework.condition.client.CheckTokenTypeIsBearer;
import io.fintechlabs.testframework.condition.client.CompareIdTokenClaims;
import io.fintechlabs.testframework.condition.client.CreateRefreshTokenRequest;
import io.fintechlabs.testframework.condition.client.EnsureAccessTokenContainsAllowedCharactersOnly;
import io.fintechlabs.testframework.condition.client.EnsureAccessTokenValuesAreDifferent;
import io.fintechlabs.testframework.condition.client.EnsureMinimumAccessTokenEntropy;
import io.fintechlabs.testframework.condition.client.EnsureMinimumRefreshTokenEntropy;
import io.fintechlabs.testframework.condition.client.EnsureMinimumRefreshTokenLength;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractRefreshTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ValidateExpiresIn;
import io.fintechlabs.testframework.condition.client.WaitForOneSecond;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;
import io.fintechlabs.testframework.sequence.ConditionSequence;

/**
 * Use the refresh token to fetch a new access token and (possibly) ID token, and compare the two.
 * The original access token and ID token should be stored as "first_access_token" and
 * "first_id_token" respectively, and there should be an environment mapping from "access_token" to
 * "second_access_token", and from "id_token" to "second_id_token".
 * See FAPIRWID2RefreshToken for an example of how to do this.
 */
public class RefreshTokenRequestSteps extends AbstractConditionSequence {

	private boolean secondClient;
	private String currentClient;
	private Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest;

	public RefreshTokenRequestSteps(boolean secondClient, Class<? extends ConditionSequence> addClientAuthenticationToTokenEndpointRequest) {
		this.secondClient = secondClient;
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

		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);

		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(CheckTokenTypeIsBearer.class, ConditionResult.FAILURE, "FAPI-R-6.2.2-1");
		callAndContinueOnFailure(EnsureMinimumAccessTokenEntropy.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-16");
		callAndContinueOnFailure(EnsureAccessTokenContainsAllowedCharactersOnly.class, ConditionResult.FAILURE, "RFC6749-A.12");
		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class);
		call(condition(ValidateExpiresIn.class)
				.skipIfObjectMissing("expires_in")
				.requirement("RFC6749-5.1")
				.dontStopOnFailure());

		callAndContinueOnFailure(EnsureAccessTokenValuesAreDifferent.class);

		callAndContinueOnFailure(ExtractIdTokenFromTokenResponse.class);

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
