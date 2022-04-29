package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class SetDpopHeaderTypToInvalidValue extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"dpop_proof_header"})
	@PostEnvironment(required = "dpop_proof_header")
	public Environment evaluate(Environment env) {

		JsonObject header = env.getObject("dpop_proof_header");

		header.addProperty("typ", "dpop+jwt+wrongyousee");

		env.putObject("dpop_proof_header", header);

		logSuccess("Added invalid 'typ' value to DPoP proof header", header);

		return env;

	}

}
