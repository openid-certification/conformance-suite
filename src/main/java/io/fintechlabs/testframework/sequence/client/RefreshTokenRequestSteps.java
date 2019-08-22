package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.AddScopeToTokenEndpointRequest;
import io.fintechlabs.testframework.condition.client.CallTokenEndpointAndReturnFullResponse;
import io.fintechlabs.testframework.condition.client.CheckForScopesInTokenResponse;
import io.fintechlabs.testframework.condition.client.CheckTokenTypeIsBearer;
import io.fintechlabs.testframework.condition.client.CreateRefreshTokenRequest;
import io.fintechlabs.testframework.condition.client.EnsureAccessTokenContainsAllowedCharactersOnly;
import io.fintechlabs.testframework.condition.client.EnsureAccessTokenValuesAreDifferent;
import io.fintechlabs.testframework.condition.client.EnsureMinimumAccessTokenEntropy;
import io.fintechlabs.testframework.condition.client.ExtractAccessTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractExpiresInFromTokenEndpointResponse;
import io.fintechlabs.testframework.condition.client.ExtractIdTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ExtractRefreshTokenFromTokenResponse;
import io.fintechlabs.testframework.condition.client.ValidateExpiresIn;
import io.fintechlabs.testframework.condition.client.WaitForOneSecond;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;
import io.fintechlabs.testframework.sequence.ConditionSequence;

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

		call(sequence(addClientAuthenticationToTokenEndpointRequest));

		//wait 1 second to make sure that iat values will be different
		callAndStopOnFailure(WaitForOneSecond.class);

		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class);

		call(exec().mapKey("access_token", "second_access_token"));
		call(exec().mapKey("id_token", "second_id_token"));
		callAndStopOnFailure(ExtractAccessTokenFromTokenResponse.class);

		callAndContinueOnFailure(CheckTokenTypeIsBearer.class, ConditionResult.FAILURE, "FAPI-R-6.2.2-1");
		callAndContinueOnFailure(EnsureMinimumAccessTokenEntropy.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-16");
		callAndContinueOnFailure(EnsureAccessTokenContainsAllowedCharactersOnly.class, ConditionResult.FAILURE, "RFC6749-A.12");
		callAndContinueOnFailure(ExtractExpiresInFromTokenEndpointResponse.class);
		call(condition(ValidateExpiresIn.class)
				.skipIfObjectMissing("expires_in")
				.requirement("RFC6749-5.1")
				.dontStopOnFailure());

		callAndContinueOnFailure(CheckForScopesInTokenResponse.class, ConditionResult.FAILURE, "FAPI-R-5.2.2-15");

		callAndContinueOnFailure(EnsureAccessTokenValuesAreDifferent.class);

		callAndContinueOnFailure(ExtractIdTokenFromTokenResponse.class);

		callAndContinueOnFailure(ExtractRefreshTokenFromTokenResponse.class, ConditionResult.INFO);

		call(exec().endBlock());
	}
}
