package net.openid.conformance.condition.client;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class ExtractCHash extends ExtractHash {

	@Override
	@PreEnvironment(required = "id_token")
	@PostEnvironment(required = "c_hash")
	public Environment evaluate(Environment env) {

		return super.extractHash(env, "c_hash", "c_hash");

	}

}
