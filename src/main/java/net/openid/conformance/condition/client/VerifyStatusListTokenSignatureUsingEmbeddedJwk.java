package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import net.openid.conformance.testmodule.Environment;

import java.text.ParseException;

/**
 * Verifies the Token Status List JWT signature using either the JWK embedded in
 * the JWT header or the issuer's server_jwks. Non-HAIP path; HAIP uses the x5c
 * header instead (see {@link ValidateStatusListTokenX5cCertificateChain}).
 *
 * Skips when no status list fetch was performed (credential had no status claim).
 */
public class VerifyStatusListTokenSignatureUsingEmbeddedJwk extends AbstractVerifyJwsSignature {

	@Override
	public Environment evaluate(Environment env) {

		if (!env.containsObject("status_list_token")) {
			log("No parsed status list token in environment, skipping signature verification");
			return env;
		}

		String statusListTokenJwtString = env.getString("status_list_token", "value");
		if (statusListTokenJwtString == null) {
			throw error("status_list_token has no raw JWT value");
		}

		SignedJWT statusListTokenJwt;
		try {
			statusListTokenJwt = SignedJWT.parse(statusListTokenJwtString);
		} catch (ParseException e) {
			throw error("Unable to parse status list token", e);
		}

		if (statusListTokenJwt.getHeader().getJWK() != null) {
			JWKSet jwkSet = new JWKSet(statusListTokenJwt.getHeader().getJWK());
			JsonObject jwkSetObject = JsonParser.parseString(jwkSet.toString()).getAsJsonObject();
			verifyJwsSignature(statusListTokenJwtString, jwkSetObject, "status list token", false, "JWT header jwk");
			return env;
		}

		if (env.containsObject("server_jwks")) {
			verifyJwsSignature(statusListTokenJwtString, env.getObject("server_jwks"), "status list token", false, "server");
			return env;
		}

		throw error("Unable to verify status list token signature because neither an embedded JWK nor server_jwks is available",
			args("header", statusListTokenJwt.getHeader().toJSONObject()));
	}
}
