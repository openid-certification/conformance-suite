package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.AddACRClaimToIdTokenClaims;
import net.openid.conformance.condition.as.AddOBIntentIdToIdTokenClaims;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class AddOpenBankingUkClaimsToAuthorizationCodeGrant extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(AddOBIntentIdToIdTokenClaims.class, "OB-5.2.2.8");
		callAndStopOnFailure(AddACRClaimToIdTokenClaims.class, "OB-5.2.2.8", "OIDCC-3.1.3.7-12");
	}

}
