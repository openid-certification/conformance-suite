package net.openid.conformance.vci10issuer.condition.clientattestation;

import com.nimbusds.jose.jwk.JWK;
import net.openid.conformance.condition.client.AbstractGenerateKey;
import net.openid.conformance.testmodule.Environment;

public class GenerateClientAttestationClientInstanceKey extends AbstractGenerateKey {

	@Override
	public Environment evaluate(Environment env) {

		JWK clientInstanceKey = super.createJwkForAlg(getClientInstanceKeyAlgorithm());
		String clientInstanceKeyJson = clientInstanceKey.toJSONString();
		env.putString("vci", "client_instance_key", clientInstanceKeyJson);
		env.putString("vci", "client_instance_key_public", clientInstanceKey.toPublicJWK().toString());

		log("Generated client_instance_key", args("client_instance_key", clientInstanceKeyJson));

		return env;
	}

	protected String getClientInstanceKeyAlgorithm() {
		return "ES256";
	}
}
