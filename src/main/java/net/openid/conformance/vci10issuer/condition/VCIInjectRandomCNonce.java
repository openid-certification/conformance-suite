package net.openid.conformance.vci10issuer.condition;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.UUID;

public class VCIInjectRandomCNonce extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		String actualCnonce = env.getString("vci", "c_nonce");
		String newCnonce = UUID.randomUUID().toString();
		env.putString("vci", "c_nonce", newCnonce);

		log("Override actual c_nonce with random value", args("actual_c_nonce", actualCnonce, "new_cnonce", newCnonce) );

		return env;
	}
}
