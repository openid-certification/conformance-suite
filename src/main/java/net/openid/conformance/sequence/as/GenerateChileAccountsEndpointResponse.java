package net.openid.conformance.sequence.as;

import net.openid.conformance.condition.as.SerializeAccountsEndpointResponseBody;
import net.openid.conformance.condition.as.SignAccountsResponseWithChileXJwsSignature;
import net.openid.conformance.sequence.AbstractConditionSequence;

/**
 * Serializes the accounts response once and signs those exact bytes with the Chile x-jws-signature,
 * bound to the request it answers (CMF-XJWS-5.3.1).
 */
public class GenerateChileAccountsEndpointResponse extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		callAndStopOnFailure(SerializeAccountsEndpointResponseBody.class);
		callAndStopOnFailure(SignAccountsResponseWithChileXJwsSignature.class,
			"CMF-XJWS-5.3.1-1", "CMF-XJWS-5.3.1-2", "CMF-XJWS-5.3.1-3", "CMF-XJWS-5.3.1-4", "CMF-XJWS-5.3.1-5");
	}
}
