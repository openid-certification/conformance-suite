package net.openid.conformance.condition.as;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateSdJwtCredential extends AbstractCreateSdJwtCredential {

	@Override
	@PostEnvironment(strings = "credential")
	public Environment evaluate(Environment env) {

		Object publicJWK = env.getElementFromObject("proof_jwt", "claims.jwt");

		String sdJwt = createSdJwt(env, publicJWK, null);
//FIXME check we're making sure that dc+sd-jwt is used in DCQL & returned credential

		env.putString("credential", sdJwt);

		log("Created an SD-JWT", args("sdjwt", sdJwt));

		return env;

	}

}
