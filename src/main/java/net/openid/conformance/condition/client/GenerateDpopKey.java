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

public class GenerateDpopKey extends AbstractGenerateKey {

	@Override
	@PreEnvironment(required = {"client", "server"})
	public Environment evaluate(Environment env) {

		String dpopSigningAlg = env.getString("client", "dpop_signing_alg");
		if(Strings.isNullOrEmpty(dpopSigningAlg)) {
			dpopSigningAlg = "PS256";
		}

		JsonElement dpopSupportedAlgs = env.getElementFromObject("server", "dpop_signing_alg_values_supported");
		if(null != dpopSupportedAlgs) {
			if(!((JsonArray)dpopSupportedAlgs).contains(new JsonPrimitive(dpopSigningAlg))) {
				dpopSigningAlg = OIDFJSON.getString(((JsonArray) dpopSupportedAlgs).get(0)); // use first alg in dpop_signing_alg_values_supported if preference is not supported
			}
		}

		JsonObject keyJson = createKeyForAlg(dpopSigningAlg);

		env.putObject("client", "dpop_private_jwk", keyJson);

		logSuccess("Generated dPOP JWKs", args("private_jwk", keyJson));

		return env;
	}

	@Override
	protected JWKGenerator<? extends JWK> onConfigure(JWKGenerator<? extends JWK> generator) {
		// Generate kid using the thumbprint so that we don't have to parse the key again to get the thumbprint
		return generator.keyIDFromThumbprint(true);
	}
}
