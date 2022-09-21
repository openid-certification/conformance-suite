package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

public class GenerateProofKey extends AbstractGenerateKey {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject keyJson = createKeyForAlg("ES256");

		env.putObject("client", "proof_jwk", keyJson);

		logSuccess("Generated Proof JWK", args("private_jwk", keyJson));

		return env;
	}

}
