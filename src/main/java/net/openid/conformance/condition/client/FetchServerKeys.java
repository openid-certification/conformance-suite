package net.openid.conformance.condition.client;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import com.google.gson.JsonSyntaxException;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;

public class FetchServerKeys extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "server_jwks")
	public Environment evaluate(Environment env) {

		if (!env.containsObject("server")) {
			throw error("No server configuration found");
		}

		JsonElement jwks = env.getElementFromObject("server", "jwks");

		if (jwks != null && jwks.isJsonObject()) {
			env.putObject("server_jwks", jwks.getAsJsonObject());
			logSuccess("Found static server JWKS", args("server_jwks", jwks));
			return env;
		} else {
			// we don't have a key yet, see if we can fetch it

			String jwksUri = env.getString("server", "jwks_uri");

			if (!Strings.isNullOrEmpty(jwksUri)) {
				// do the fetch

				log("Fetching server key", args("jwks_uri", jwksUri));

				try {
					RestTemplate restTemplate = createRestTemplate(env);

					String jwkString = restTemplate.getForObject(jwksUri, String.class);

					log("Found JWK set string", args("jwk_string", jwkString));

					JsonObject jwkSet = new JsonParser().parse(jwkString).getAsJsonObject();
					env.putObject("server_jwks", jwkSet);

					logSuccess("Found server JWK set", args("server_jwks", jwkSet));
					return env;

				} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
					throw error("Error creating HTTP client", e);
				} catch (RestClientException e) {
					throw error("Exception while fetching server key", e);
				} catch (JsonSyntaxException e) {
					throw error("Server JWKs set string is not JSON", e);
				}

			} else {
				throw error("Didn't find a JWKS or a JWKS URI in the server configuration");
			}

		}

	}

}
