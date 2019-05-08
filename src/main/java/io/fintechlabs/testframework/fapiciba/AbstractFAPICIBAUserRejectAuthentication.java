package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.client.CallAutomatedCibaApprovalEndpoint;

public abstract class AbstractFAPICIBAUserRejectAuthentication extends AbstractFAPICIBAWithMTLS {

	@Override
	protected void callAutomatedEndpoint() {
		env.putString("request_action", "deny");
		callAndStopOnFailure(CallAutomatedCibaApprovalEndpoint.class);
	}
}
