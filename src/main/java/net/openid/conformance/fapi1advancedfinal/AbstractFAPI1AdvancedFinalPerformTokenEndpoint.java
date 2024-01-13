package net.openid.conformance.fapi1advancedfinal;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;

/**
 * This class finished the test after the token endpoint call
 *
 * i.e. it does not go on to call the resource endpoint.
 */
public abstract class AbstractFAPI1AdvancedFinalPerformTokenEndpoint extends AbstractFAPI1AdvancedFinalServerTestModule {

	@Override
	protected void callTokenEndpoint() {
		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "FAPI1-BASE-5.2.2-19");
	}

	@Override
	protected void performPostAuthorizationFlow() {

		// call the token endpoint and complete the flow
		createAuthorizationCodeRequest();

		requestAuthorizationCode();

	}

}
