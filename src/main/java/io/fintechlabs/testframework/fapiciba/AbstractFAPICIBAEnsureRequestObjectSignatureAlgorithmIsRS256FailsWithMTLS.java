package io.fintechlabs.testframework.fapiciba;

import io.fintechlabs.testframework.condition.Condition;
import io.fintechlabs.testframework.condition.client.AddAlgorithmAsRS256;

public abstract class AbstractFAPICIBAEnsureRequestObjectSignatureAlgorithmIsRS256FailsWithMTLS extends AbstractFAPICIBAEnsureSendingInvalidBackchannelAuthorisationRequestWithMTLS {

	@Override
	protected void performAuthorizationRequest() {
		callAndContinueOnFailure(AddAlgorithmAsRS256.class, Condition.ConditionResult.FAILURE, "CIBA-7.2");

		super.performAuthorizationRequest();
	}
}
