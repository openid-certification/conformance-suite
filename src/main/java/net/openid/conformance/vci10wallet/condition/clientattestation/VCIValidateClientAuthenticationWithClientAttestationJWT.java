package net.openid.conformance.vci10wallet.condition.clientattestation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class VCIValidateClientAuthenticationWithClientAttestationJWT extends AbstractConditionSequence {
	@Override
	public void evaluate() {

		callAndStopOnFailure(ExtractClientAttestationFromRequest.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-6.2");
		callAndStopOnFailure(ValidateClientAttestationKeyBindingSignature.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-6.2");
		callAndStopOnFailure(CheckForClientAttestationProofJwtReuse.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-10.2", "OAuth2-ATCA07-5.2");
		callAndStopOnFailure(ValidateClientAttestationIssuer.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-6.2");
		callAndStopOnFailure(ValidateClientAttestationProofJwtAudience.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA07-5.2-5.2.1");
	}
}
