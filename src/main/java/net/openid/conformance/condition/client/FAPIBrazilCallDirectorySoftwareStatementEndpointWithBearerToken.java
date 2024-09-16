package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonObject;
import com.nimbusds.jose.JOSEException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
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


public class FAPIBrazilCallDirectorySoftwareStatementEndpointWithBearerToken extends AbstractCondition {

	@Override
	@PreEnvironment(required = { "directory_access_token", "config", "certificate_subject" })
	@PostEnvironment(required = {  "software_statement_assertion" })
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

		// https://matls-api.sandbox.directory.openbankingbrasil.org.br/organisations/${ORGID}/softwarestatements/${SSID}/assertion
		String apibase = env.getString("config", "directory.apibase");
		if (Strings.isNullOrEmpty(apibase)) {
			throw error("directory.apibase missing from test configuration");
		}
		if (!apibase.endsWith("/")) {
			apibase += "/";
		}
		String ou = env.getString("certificate_subject", "ou");
		if (Strings.isNullOrEmpty(ou)) {
			throw error("'ou' not found in TLS certificate subject");
		}
		String cn = env.getString("certificate_subject", "brazil_software_id");
		if (Strings.isNullOrEmpty(cn)) {
			throw error("'cn' not found in TLS certificate subject");
		}

		String resourceEndpoint = "%sorganisations/%s/softwarestatements/%s/assertion".formatted(apibase, ou, cn);

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
