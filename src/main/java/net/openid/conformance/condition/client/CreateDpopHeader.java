package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.text.ParseException;

public class CreateDpopHeader extends AbstractCondition {

	private JsonObject getPublicJwk(JsonObject jwk) {
		JWK parsedJwk = null;
		try {
			parsedJwk = JWK.parse(jwk.toString());
		} catch (ParseException e) {
			throw error("Invalid DPoP JWK", e, args("jwk", jwk));
		}
		JsonObject pubObj = JsonParser.parseString(parsedJwk.toPublicJWK().toString()).getAsJsonObject();

		return pubObj;
	}

	@Override
	@PreEnvironment(required = {"client"})
	@PostEnvironment(required = "dpop_proof_header")
	public Environment evaluate(Environment env) {

		JsonObject jwk = env.getElementFromObject("client", "dpop_private_jwk").getAsJsonObject();

		JsonObject pubObj = getPublicJwk(jwk);

		JsonObject header = new JsonObject();

		header.addProperty("alg", OIDFJSON.getString(jwk.get("alg"))); // use alg in jwk
		header.addProperty("typ", "dpop+jwt");
		header.add("jwk", pubObj);

		env.putObject("dpop_proof_header", header);

		logSuccess("Created DPoP proof header", header);

		return env;

	}

}
