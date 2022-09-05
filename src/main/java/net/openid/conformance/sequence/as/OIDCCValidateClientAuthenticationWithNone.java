package net.openid.conformance.sequence.as;

import net.openid.conformance.sequence.AbstractConditionSequence;

public class OIDCCValidateClientAuthenticationWithNone extends AbstractConditionSequence {

	@Override
	public void evaluate() {
		//do nothing for now
		//we don't know if this should actually perform some checks, yet.
		//we may update this class once we clarify that
		//this class may also turn out to be unnecessary
		//but we don't know that yet because the tests are still incomplete
	}
}
