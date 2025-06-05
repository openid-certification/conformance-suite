package net.openid.conformance.vciid2wallet.condition.clientattestation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class VCIValidateClientAuthenticationWithClientAttestationJWT extends AbstractConditionSequence {
	@Override
	public void evaluate() {

		callAndStopOnFailure(ExtractClientAttestationFromRequest.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA05-6.2");
		callAndStopOnFailure(ValidateClientAttestation.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA05-6.2");
	}
}
