package net.openid.conformance.vciid2issuer.condition.clientattestation;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.gen.JWKGenerator;
import net.openid.conformance.condition.client.AbstractGenerateKey;
import net.openid.conformance.testmodule.Environment;

public class GenerateClientAttestationClientInstanceKey extends AbstractGenerateKey {

	@Override
	public Environment evaluate(Environment env) {

		// TODO make alg configurable
		JWK clientInstanceKey = this.createJwkForAlg("ES256");
		String clientInstanceKeyJson = clientInstanceKey.toJSONString();
		env.putString("vci", "client_attestation_key_id", clientInstanceKey.getKeyID());
		env.putString("vci", "client_attestation_key", clientInstanceKeyJson);

		log("Generated client_attestation_key", args("client_attestation_key", clientInstanceKeyJson));

		return env;
	}

	@Override
	protected JWKGenerator<? extends JWK> onConfigure(JWKGenerator<? extends JWK> generator) {
		generator.keyID("clientInstanceKey");
		return generator;
	}
}
