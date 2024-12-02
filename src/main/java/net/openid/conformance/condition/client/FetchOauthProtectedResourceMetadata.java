package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

public class FetchOauthProtectedResourceMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "oauth_protected_resource_metadata")
	public Environment evaluate(Environment env) {

		String oauthResourceMedatataUrl = env.getString("server", "oauth_protected_resource_metadata_uri");

		if (!Strings.isNullOrEmpty(oauthResourceMedatataUrl)) {
			// do the fetch

			log("Fetching server key", args("oauth_protected_resource_metadata_uri", oauthResourceMedatataUrl));

			try {
				RestTemplate restTemplate = createRestTemplate(env);

				ResponseEntity<String> oauthProtectedResourceMetadataResponse = restTemplate.getForEntity(oauthResourceMedatataUrl, String.class);
				if (!HttpStatus.OK.equals(oauthProtectedResourceMetadataResponse.getStatusCode())) {
					throw error("Protected OAuth resource metadata could not be fetched", args("status_code", oauthProtectedResourceMetadataResponse.getStatusCode().value()));
				}

				String oauthProtectedResourceMetadata = oauthProtectedResourceMetadataResponse.getBody();
				if (oauthProtectedResourceMetadata == null) {
					throw error("Did not find oauth_protected_resource_metadata");
				}

				log("Found OAuth protected resource metadata set string", args("oauth_protected_resource_metadata", oauthProtectedResourceMetadata));

				JsonObject oauthProtectedResourceMetadataJsonObject = JsonParser.parseString(oauthProtectedResourceMetadata).getAsJsonObject();
				env.putObject("oauth_protected_resource_metadata", oauthProtectedResourceMetadataJsonObject);

				logSuccess("Found server OAuth protected resource metadata", args("oauth_protected_resource_metadata", oauthProtectedResourceMetadataJsonObject));
				return env;

			} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
				throw error("Error creating HTTP client", e);
			} catch (RestClientException e) {
				String msg = "Fetching OAuth Protected Resource metadata from " + oauthResourceMedatataUrl + " failed";
				if (e.getCause() != null) {
					msg += " - " +e.getCause().getMessage();
				}
				throw error(msg, e);
			} catch (JsonSyntaxException e) {
				throw error("Server OAuth Protected Resource metadata string is not JSON", e);
			}

		} else {
			throw error("Didn't find oauth_protected_resource_metadata_uri in the server configuration");
		}

	}

}
