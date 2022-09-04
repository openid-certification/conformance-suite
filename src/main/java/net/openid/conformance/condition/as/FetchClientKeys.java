package net.openid.conformance.condition.as;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

public class FetchClientKeys extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		String jwksUri = env.getString("client", "jwks_uri");
		JsonObject client = env.getObject("client");
		if(client.has("jwks")) {
			throw error("Client already has a jwks", args("client", client));
		}
		if (!Strings.isNullOrEmpty(jwksUri)) {
			// do the fetch

			log("Fetching client keys", args("jwks_uri", jwksUri));

			try {
				RestTemplate restTemplate = createRestTemplate(env);

				String jwkString = restTemplate.getForObject(jwksUri, String.class);

				log("Found JWK set string", args("jwk_string", jwkString));

				JsonObject jwkSet = JsonParser.parseString(jwkString).getAsJsonObject();

				client.add("jwks", jwkSet);
				env.putObject("client", client);

				logSuccess("Downloaded and added client JWK set to client", args("client", client));
				return env;

			} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
				throw error("Error creating HTTP client", e);
			} catch (RestClientException e) {
				String msg = "Unable to fetch client keys from " + jwksUri;
				if (e.getCause() != null) {
					msg += " - " +e.getCause().getMessage();
				}
				throw error(msg, e);
			} catch (JsonSyntaxException e) {
				throw error("Client JWKs set string is not JSON", e);
			}

		} else {
			throw error("Didn't find a jwks_uri in client configuration");
		}

	}
}
