package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import java.util.Collections;

public class UnregisterDynamicallyRegisteredClient extends AbstractCondition {

	private static final Logger logger = LoggerFactory.getLogger(UnregisterDynamicallyRegisteredClient.class);

	@Override
	@PreEnvironment(required = "client")
	public Environment evaluate(Environment env) {

		String accessToken = env.getString("client", "registration_access_token");
		if (Strings.isNullOrEmpty(accessToken)){
			throw error("Couldn't find registration access token.");
		}

		String registrationClientUri = env.getString("client", "registration_client_uri");
		if (Strings.isNullOrEmpty(registrationClientUri)){
			throw error("Couldn't find registration client uri");
		}

		try {

			RestTemplate restTemplate = createRestTemplate(env);
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
			headers.set("Authorization", String.join(" ", "Bearer", accessToken));

			HttpEntity<?> request = new HttpEntity<>(headers);
			try {
				ResponseEntity<?> response = restTemplate.exchange(registrationClientUri, HttpMethod.DELETE, request, String.class);
				if (response.getStatusCode() == HttpStatus.NO_CONTENT) {
					logSuccess("Client successfully unregistered");
				}
			} catch (RestClientResponseException e) {
				throw error("Error from the registration client endpoint", e, args("code", e.getRawStatusCode(), "status", e.getStatusText(), "body", e.getResponseBodyAsString()));
			}

		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException | KeyStoreException | IOException | UnrecoverableKeyException e) {
			logger.warn("Error creating HTTP Client", e);
			throw error("Error creating HTTP Client", e);
		}

		return env;
	}
}
