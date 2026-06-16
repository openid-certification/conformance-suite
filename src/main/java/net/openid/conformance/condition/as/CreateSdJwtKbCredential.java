package net.openid.conformance.condition.as;

import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.PreGeneratedJwks;

public class CreateSdJwtKbCredential extends AbstractCreateSdJwtCredential {

	@Override
	@PostEnvironment(strings = {"credential", "holder_private_jwk"})
	public Environment evaluate(Environment env) {

		// Create a private key for the credential key binding
		ECKey privateKey = PreGeneratedJwks.nextEcKey(env, Curve.P_256);
		String sdJwt = createSdJwt(env, privateKey.toPublicJWK(), privateKey, "urn:eudi:pid:1");

		env.putString("credential", sdJwt);
		env.putString("holder_private_jwk", privateKey.toJSONString());

		log("Created an SD-JWT+KB", args("sdjwt", sdJwt));

		return env;

	}

}
