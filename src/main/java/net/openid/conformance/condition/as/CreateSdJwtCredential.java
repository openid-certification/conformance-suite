package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateSdJwtCredential extends AbstractCreateSdJwtCredential {

	@Override
	@PostEnvironment(strings = "credential")
	public Environment evaluate(Environment env) {

		Object publicJWK = env.getElementFromObject("proof_jwt", "header.jwk");
		if (publicJWK == null) {
			throw error("Couldn't find public JWK in proof_jwt header.jwk");
		}

		String sdJwt = createSdJwt(env, publicJWK, null);

		env.putString("credential", sdJwt);

		log("Created an EU ARF 1.8 PID in SD-JWT VC format", args("sdjwt", sdJwt));

		return env;

	}

}
