package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

public class AddClientX509CertificateClaimToPublicJWKs extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"client_public_jwks", "mutual_tls_authentication"})
	@PostEnvironment(required = "client_public_jwks")
	public Environment evaluate(Environment env) {

		String certString = env.getString("mutual_tls_authentication", "cert");

		if (Strings.isNullOrEmpty(certString)) {
			throw error("Couldn't find TLS client certificate for MTLS");
		}

		JsonObject publicJwks = env.getObject("client_public_jwks");
		JsonArray keys = publicJwks.getAsJsonArray("keys");
		JsonObject jwk = keys.get(0).getAsJsonObject();

		JsonArray certs = new JsonArray();
		certs.add(certString);
		jwk.add("x5c", certs);

		logSuccess("Added x5c claim to public JWKs", args("client_public_jwks", publicJwks));

		return env;
	}

}
