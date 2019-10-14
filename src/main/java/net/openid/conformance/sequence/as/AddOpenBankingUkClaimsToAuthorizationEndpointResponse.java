package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.AddOBIntentIdToIdTokenClaims;
import net.openid.conformance.condition.as.ExtractOBIntentId;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class AddOpenBankingUkClaimsToAuthorizationEndpointResponse extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(ExtractOBIntentId.class, "OB-5.2.2.8");
		callAndStopOnFailure(AddOBIntentIdToIdTokenClaims.class, "OB-5.2.2.8");
	}

}
