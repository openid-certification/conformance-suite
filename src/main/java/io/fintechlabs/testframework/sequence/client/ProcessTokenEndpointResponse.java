package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.*;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

public class ProcessTokenEndpointResponse extends AbstractConditionSequence {
	@Override
	public void evaluate() {
		call(condition(CheckIfTokenEndpointResponseError.class));

		call(condition(CheckForAccessTokenValue.class));

		call(condition(ExtractAccessTokenFromTokenResponse.class));

		call(condition(CheckForScopesInTokenResponse.class).dontStopOnFailure().onFail(Condition.ConditionResult.WARNING));

		call(condition(ExtractIdTokenFromTokenResponse.class));

		call(condition(ValidateIdToken.class));

		call(condition(ValidateIdTokenSignature.class));

		call(condition(CheckForSubjectInIdToken.class));

		call(condition(CheckForRefreshTokenValue.class).dontStopOnFailure().onFail(Condition.ConditionResult.WARNING));

		call(condition(EnsureMinimumTokenLength.class).dontStopOnFailure().onFail(Condition.ConditionResult.WARNING));

		call(condition(EnsureMinimumTokenEntropy.class).dontStopOnFailure().onFail(Condition.ConditionResult.WARNING));

	}
}
