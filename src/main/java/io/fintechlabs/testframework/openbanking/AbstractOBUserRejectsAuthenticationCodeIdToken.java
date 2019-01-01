package io.fintechlabs.testframework.openbanking;

import io.fintechlabs.testframework.condition.Condition.ConditionResult;
import io.fintechlabs.testframework.condition.client.CheckForAuthorizationEndpointErrorInQueryForHybridFLow;
import io.fintechlabs.testframework.condition.client.ExpectAccessDeniedErrorFromAuthorizationEndpoint;
import io.fintechlabs.testframework.condition.client.RejectAuthCodeInUrlQuery;
import io.fintechlabs.testframework.condition.client.ValidateErrorResponseFromAuthorizationEndpoint;

/**
 * @author ddrysdale
 *
 */

public abstract class AbstractOBUserRejectsAuthenticationCodeIdToken extends AbstractOBServerTestModuleCodeIdToken {

	@Override
	protected void createAuthorizationRequest() {

		env.putInteger("requested_state_length", 128);

		super.createAuthorizationRequest();
	}

	@Override
	protected void onAuthorizationCallbackResponse() {

		callAndContinueOnFailure(RejectAuthCodeInUrlQuery.class, ConditionResult.FAILURE, "OIDCC-3.3.2.5");

		skipIfMissing(new String[] { "callback_query_params" }, null, ConditionResult.INFO,
			CheckForAuthorizationEndpointErrorInQueryForHybridFLow.class, ConditionResult.FAILURE, "OIDCC-3.3.2.6");


		callAndContinueOnFailure(ValidateErrorResponseFromAuthorizationEndpoint.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");
		callAndContinueOnFailure(ExpectAccessDeniedErrorFromAuthorizationEndpoint.class, ConditionResult.FAILURE, "OIDCC-3.1.2.6");

		fireTestFinished();
	}

}
