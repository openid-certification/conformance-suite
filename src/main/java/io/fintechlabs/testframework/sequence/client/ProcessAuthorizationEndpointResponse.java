package io.fintechlabs.testframework.sequence.client;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.*;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

public class ProcessAuthorizationEndpointResponse extends AbstractConditionSequence {
	@Override
	public void evaluate() {

		call(exec().mapKey("callback_params", "implicit_callback_params"));

		call(condition(ExtractImplicitHashToCallbackResponse.class));

		// TODO: key for processing implicit parameters here; for now just unmap
		call(exec().unmapKey("callback_params"));

		call(exec().mapKey("callback_params", "callback_query_params"));

		call(condition(CheckIfAuthorizationEndpointError.class));

		call(condition(CheckMatchingStateParameter.class));

		call(condition(ExtractAuthorizationCodeFromAuthorizationResponse.class));

		call(condition(CreateTokenEndpointRequestForAuthorizationCodeGrant.class));

		runAccessory("client_token_endpoint_authenticaiton", condition(AddBasicAuthClientSecretAuthenticationParameters.class));

		runAccessory("token_endpoint_request");

		call(condition(CallTokenEndpoint.class));

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
