package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public class VCIWaitForTxCode extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		log("Waiting to receive tx_code via GET on /tx_code endpoint, see exposed values.");

		return env;
	}
}
