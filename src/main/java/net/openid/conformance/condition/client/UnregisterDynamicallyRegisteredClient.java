package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import java.util.Collections;

public class UnregisterDynamicallyRegisteredClient extends AbstractCondition {

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		String accessToken = env.getString("client", "registration_access_token");
		if (Strings.isNullOrEmpty(accessToken)){
			log("Couldn't find registration_access_token.");
			return env;
		}

		String registrationClientUri = env.getString("client", "registration_client_uri");
		if (Strings.isNullOrEmpty(registrationClientUri)){
			log("Couldn't find registration_client_uri.");
			return env;
		}

		try {

			RestTemplate restTemplate = createRestTemplate(env);
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.set("Authorization", "Bearer " + accessToken);

			HttpEntity<?> request = new HttpEntity<>(headers);
			try {
				ResponseEntity<?> response = restTemplate.exchange(registrationClientUri, HttpMethod.DELETE, request, String.class);
				if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
					throw error("registration_client_uri returned a http status code other than 204 No Content",
						args("code", response.getStatusCode()));
				}
			} catch (RestClientResponseException e) {
				throw error("Error when calling registration_client_uri", args("code", e.getStatusCode().value(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			} catch (RestClientException e) {
				String msg = "Call to registration client uri " + registrationClientUri + " failed";
				if (e.getCause() != null) {
					msg += " - " +e.getCause().getMessage();
				}
				throw error(msg, e);
			}

		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}

		logSuccess("Client successfully unregistered");
		return env;
	}
}
