package io.fintechlabs.testframework.sequence.as;

import io.fintechlabs.testframework.condition.as.AddOBIntentIdToIdTokenClaims;
import io.fintechlabs.testframework.condition.as.ExtractOBIntentId;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

public class AddOpenBankingUkClaimsToAuthorizationEndpointResponse extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(ExtractOBIntentId.class, "OB-5.2.2.8");
		callAndStopOnFailure(AddOBIntentIdToIdTokenClaims.class, "OB-5.2.2.8");
	}

}
