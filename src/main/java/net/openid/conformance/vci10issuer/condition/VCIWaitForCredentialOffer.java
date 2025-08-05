package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIWaitForCredentialOffer extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		log("Waiting for call to credential offer endpoint, see exposed values.");

		return env;
	}
}
