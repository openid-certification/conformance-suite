package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class OIDSSFValidateSecurityEventTokenTxnClaim extends AbstractCondition {

	@PreEnvironment(required = {"set_token"})
	@Override
	public Environment evaluate(Environment env) {

		JsonElement txnEl = env.getElementFromObject("set_token", "claims.txn");
		if (txnEl != null) {
			log("Found optional 'txn' claim", args("txn", txnEl));
		} else {
			log("Did not find optional 'txn' claim");
		}

		return env;
	}
}
