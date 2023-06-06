package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractVerifyJwsSignature;
import net.openid.conformance.testmodule.Environment;

public class FAPIBrazilValidateSSASignature extends AbstractVerifyJwsSignature {

	@Override
	@PreEnvironment(strings = { "software_statement_assertion"}, required = {"directory_ssa_jwks"})
	public Environment evaluate(Environment env) {

		String ssa = env.getString("software_statement_assertion");
		JsonObject jwks = env.getObject("directory_ssa_jwks");
		verifyJwsSignature(ssa, jwks, "software statement", true, "directory ssa");
		return env;
	}
}
