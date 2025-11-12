package net.openid.conformance.condition.client;

import com.google.gson.JsonElement;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.X509CertUtils;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

import java.security.cert.X509Certificate;
import java.util.List;

public class SetClientIdToX509Hash extends AbstractGetSigningKey {

	@Override
	@PreEnvironment(required = { "config" })
	public Environment evaluate(Environment env) {

		JsonElement jwks = env.getElementFromObject("config", "client.jwks");
		if (jwks == null || !jwks.isJsonObject()) {
			throw error("client.jwks is missing from configuration or not a JSON object");
		}
		JWK signingJwk = getSigningKey("signing", jwks.getAsJsonObject());

		List<Base64> x5c = signingJwk.getX509CertChain();
		if (x5c == null || x5c.isEmpty()) {
			throw error("An x509 certificate is needed for this client authentication method, but the signing key in the client jwks in the configuration doesn't have an x5c entry", args("client_jwks", jwks));
		}
		Base64 certBase64 = x5c.get(0);
		X509Certificate cert = X509CertUtils.parse(certBase64.decode());
		String clientId = X509CertUtils.computeSHA256Thumbprint(cert).toString();

		env.putString("config", "client.client_id", clientId);

		log("Set client_id to x509 hash",
			args("client_id", clientId,
				"x5c_certificate", certBase64.toString()));

		return env;
	}

}
