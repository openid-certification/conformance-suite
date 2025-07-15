package net.openid.conformance.vci10issuer.condition.clientattestation;

import net.openid.conformance.condition.Condition;
import net.openid.conformance.sequence.AbstractConditionSequence;

public class AddClientAttestationClientAuthToEndpointRequest extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(CreateClientAttestationProofJwt.class, Condition.ConditionResult.FAILURE, "OAuth2-ATCA05-1");
		callAndStopOnFailure(AddClientAttestationHeaderToRequest.class, "OAuth2-ATCA05-6.1");
		callAndStopOnFailure(AddClientAttestationProofHeaderToRequest.class, "OAuth2-ATCA05-6.1");
	}
}
