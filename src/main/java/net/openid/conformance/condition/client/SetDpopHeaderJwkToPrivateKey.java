package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

public class SetDpopHeaderJwkToPrivateKey extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client"})
	@PostEnvironment(required = "dpop_proof_header")
	public Environment evaluate(Environment env) {

		JsonObject jwk = env.getElementFromObject("client", "dpop_private_jwk").getAsJsonObject();

		JsonObject header = env.getObject("dpop_proof_header");

		header.add("jwk", jwk);

		env.putObject("dpop_proof_header", header);

		logSuccess("Added private jwk to DPoP proof header", header);

		return env;

	}

}
