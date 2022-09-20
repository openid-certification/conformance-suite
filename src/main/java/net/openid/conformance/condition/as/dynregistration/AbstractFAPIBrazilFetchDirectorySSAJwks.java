package net.openid.conformance.condition.as.dynregistration;

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

public abstract class AbstractFAPIBrazilFetchDirectorySSAJwks extends AbstractCondition {
	@Override
	@PreEnvironment(required = {"config"})
	@PostEnvironment(required = {"directory_ssa_jwks"})
	public Environment evaluate(Environment env) {

		String directoryBase = env.getString("config", "directory.keystore");
		String jwksUri = directoryBase + getJwksPath();
		log("Fetching directory jwks", args("jwks_uri", jwksUri));

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			String jwkString = restTemplate.getForObject(jwksUri, String.class);

			log("Found JWK set string", args("jwk_string", jwkString));

			JsonObject jwkSet = JsonParser.parseString(jwkString).getAsJsonObject();


			env.putObject("directory_ssa_jwks", jwkSet);

			logSuccess("Downloaded and added directory SSA JWK set to environment",
				args("directory_ssa_jwks", jwkSet));
			return env;

		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException |
				 NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Unable to fetch jwks from " + jwksUri;
			if (e.getCause() != null) {
				msg += " - " + e.getCause().getMessage();
			}
			throw error(msg, e);
		} catch (JsonSyntaxException e) {
			throw error("Directory SSA JWKs set string is not JSON", e);
		}
	}

	protected abstract String getJwksPath();
}
