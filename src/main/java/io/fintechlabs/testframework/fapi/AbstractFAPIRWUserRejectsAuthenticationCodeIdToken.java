package io.fintechlabs.testframework.fapi;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.CheckForAuthorizationEndpointErrorInQueryForHybridFLow;
import io.fintechlabs.testframework.condition.client.ExpectAccessDeniedErrorFromAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.RejectAuthCodeInUrlQuery;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;

public abstract class AbstractFAPIRWUserRejectsAuthenticationCodeIdToken extends AbstractFAPIRWServerTestModule {

	@Override
	protected void createAuthorizationRequest() {

		env.putInteger("requested_state_length", 128);

		super.createAuthorizationRequest();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		skipIfMissing(new String[] { "callback_query_params" }, null, Condition.ConditionResult.INFO,
			CheckForAuthorizationEndpointErrorInQueryForHybridFLow.class, Condition.ConditionResult.FAILURE, "OIDCC-3.3.2.6");


		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(ExpectAccessDeniedErrorFromAuthorizationEndpoint.class, Condition.ConditionResult.FAILURE, "OIDCC-3.1.2.6");

		fireTestFinished();
	}

}
