package net.openid.conformance.condition.rs;

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

public class FAPIBrazilFetchClientOrganizationJwksFromDirectory extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"config", "client_certificate_subject"})
	@PostEnvironment(required = "client_organization_jwks")
	public Environment evaluate(Environment env) {
		String keystore = getStringFromEnvironment(env, "config", "directory.keystore",
			"Directory Keystore base in test configuration");

		String orgId = env.getString("client_certificate_subject", "ou");
		if (Strings.isNullOrEmpty(orgId)) {
			throw error("Client organization id is unknown; this needs to be extracted from client certificate",
				args("client_certificate_subject", env.getObject("client_certificate_subject")));
		}

		String orgJwksUri = keystore + orgId + "/application.jwks";

		log("Fetching client organization keys", args("jwks_uri", orgJwksUri));

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			String jwkString = restTemplate.getForObject(orgJwksUri, String.class);

			log("Found JWK set string", args("jwk_string", jwkString));

			JsonObject jwkSet = JsonParser.parseString(jwkString).getAsJsonObject();


			env.putObject("client_organization_jwks", jwkSet);

			logSuccess("Downloaded and added client organization JWK set to environment",
				args("client_organization_jwks", jwkSet));
			return env;

		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Unable to fetch client keys from " + orgJwksUri;
			if (e.getCause() != null) {
				msg += " - " +e.getCause().getMessage();
			}
			throw error(msg, e);
		} catch (JsonSyntaxException e) {
			throw error("Client organization JWKs set string is not JSON", e);
		}


	}
}
