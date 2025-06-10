package net.openid.conformance.vciid2issuer.condition.clientattestation;

import net.openid.conformance.sequence.AbstractConditionSequence;

public class AddClientAttestationHeaderToTokenEndpointRequest extends AbstractConditionSequence {

	@Override
	public void evaluate() {

		callAndStopOnFailure(AddClientAttestationHeaderToRequest.class, "OAuth2-ATCA05-6.1");
		callAndStopOnFailure(AddClientAttestationProofHeaderToRequest.class, "OAuth2-ATCA05-6.1");
	}
}
