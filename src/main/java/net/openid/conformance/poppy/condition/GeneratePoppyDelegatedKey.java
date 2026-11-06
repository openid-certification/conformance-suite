package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class GeneratePoppyDelegatedKey extends AbstractCondition {

	@Override
	@PostEnvironment(required = "poppy_delegated_key")
	public Environment evaluate(Environment env) {
		ECKey key;
		try {
			key = new ECKeyGenerator(Curve.P_256)
				.algorithm(JWSAlgorithm.ES256)
				.keyIDFromThumbprint(true)
				.generate();
		} catch (JOSEException e) {
			throw error("Failed to generate the delegated key", e);
		}

		JsonObject json = JsonParser.parseString(key.toJSONString()).getAsJsonObject();
		env.putObject("poppy_delegated_key", json);

		logSuccess("Generated the delegated EC P-256 key", args("poppy_delegated_key", json));
		return env;
	}
}
