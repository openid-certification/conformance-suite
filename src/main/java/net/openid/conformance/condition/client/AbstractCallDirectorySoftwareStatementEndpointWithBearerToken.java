package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.util.JWTUtil;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.text.ParseException;
import java.util.Collections;

/**
 * Retrieves a software statement from an ecosystem directory with a GET, using the directory access
 * token in {@code directory_access_token}, and stores it in {@code software_statement_assertion}.
 * Subclasses say where the directory's software statement endpoint is.
 */
public abstract class AbstractCallDirectorySoftwareStatementEndpointWithBearerToken extends AbstractCondition {

	protected abstract String getSoftwareStatementEndpoint(Environment env);

	@Override
	public Environment evaluate(Environment env) {
		// Note that this code doesn't follow the prefered pattern and should be refactored; extending CallProtectedResource is prefered

		String accessToken = env.getString("directory_access_token", "value");
		if (Strings.isNullOrEmpty(accessToken)) {
			throw error("Access token not found");
		}

		String tokenType = env.getString("directory_access_token", "type");
		if (Strings.isNullOrEmpty(tokenType)) {
			throw error("Token type not found");
		} else if (!tokenType.equalsIgnoreCase("Bearer")) {
			throw error("Access token is not a bearer token", args("token_type", tokenType));
		}

		String resourceEndpoint = getSoftwareStatementEndpoint(env);

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			HttpHeaders headers = new HttpHeaders();

			headers.setAccept(Collections.singletonList(DATAUTILS_MEDIATYPE_APPLICATION_JWT_UTF8));
			headers.set("Authorization", "Bearer " + accessToken);

			HttpEntity<String> request = new HttpEntity<>(null, headers);

			ResponseEntity<String> response = restTemplate.exchange(resourceEndpoint, HttpMethod.GET, request, String.class);

			String jsonString = response.getBody();

			if (Strings.isNullOrEmpty(jsonString)) {
				throw error("Empty/missing response from the software statement endpoint");
			} else {
				log("software statement endpoint response", args("response", jsonString));

				JsonObject jwtAsJsonObject;
				try {
					jwtAsJsonObject = JWTUtil.jwtStringToJsonObjectForEnvironment(jsonString, null, null);
				} catch (JOSEException | ParseException e) {
					throw error("Couldn't parse software statement as a JWT", args("ssa", jsonString, "error", e.getMessage()));
				}
				if (jwtAsJsonObject == null) {
					throw error("Couldn't parse software statement as a JWT", args("ssa", jsonString));
				}


				env.putObject("software_statement_assertion", jwtAsJsonObject);

				logSuccess("Parsed assertion endpoint response", jwtAsJsonObject);

				return env;
			}
		} catch (RestClientResponseException e) {
			throw error("Error from the software statement endpoint", args("code", e.getStatusCode().value(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		} catch (RestClientException e) {
			String msg = "Call to software statement endpoint " + resourceEndpoint + " failed";
			if (e.getCause() != null) {
				msg += " - " +e.getCause().getMessage();
			}
			throw error(msg, e);
		}

	}

}
