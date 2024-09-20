package net.openid.conformance.fapirwid2;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.condition.client.CallTokenEndpointAndReturnFullResponse;

/**
 * This class uses to perform only steps that call to token endpoint
 */
public abstract class AbstractFAPIRWID2PerformTokenEndpoint extends AbstractFAPIRWID2ServerTestModule {

	@Override
	protected void callTokenEndpoint() {
		callAndStopOnFailure(CallTokenEndpointAndReturnFullResponse.class, Condition.ConditionResult.FAILURE, "FAPI-R-5.2.2-19");
	}

	@Override
	protected void performPostAuthorizationFlow() {

		// call the token endpoint and complete the flow
		createAuthorizationCodeRequest();

		requestAuthorizationCode();

	}

}
