package net.openid.conformance.condition.as;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.Curve;
import com.nimbusds.jose.jwk.ECKey;
import com.nimbusds.jose.jwk.gen.ECKeyGenerator;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;

public class CreateSdJwtKbCredential extends AbstractCreateSdJwtCredential {

	@Override
	@PostEnvironment(strings = "credential")
	public Environment evaluate(Environment env) {

		// Create a private key for the credential key binding
		ECKey privateKey = null;
		try {
			privateKey = new ECKeyGenerator(Curve.P_256).generate();
		} catch (JOSEException e) {
			throw new RuntimeException(e);
		}
		String sdJwt = createSdJwt(env, privateKey.toPublicJWK(), privateKey);

		env.putString("credential", sdJwt);

		log("Created an SD-JWT+KB", args("sdjwt", sdJwt));

		return env;

	}

}
