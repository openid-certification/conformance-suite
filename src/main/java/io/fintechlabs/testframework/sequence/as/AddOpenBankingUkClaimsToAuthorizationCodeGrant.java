package io.fintechlabs.testframework.sequence.as;

import io.fintechlabs.testframework.condition.as.AddACRClaimToIdTokenClaims;
import io.fintechlabs.testframework.condition.as.AddOBIntentIdToIdTokenClaims;
import io.fintechlabs.testframework.sequence.AbstractConditionSequence;

public class AddOpenBankingUkClaimsToAuthorizationCodeGrant extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddOBIntentIdToIdTokenClaims.class, "OB-5.2.2.8");
		callAndStopOnFailure(AddACRClaimToIdTokenClaims.class, "OB-5.2.2.8", "OIDCC-3.1.3.7-12");
	}

}
