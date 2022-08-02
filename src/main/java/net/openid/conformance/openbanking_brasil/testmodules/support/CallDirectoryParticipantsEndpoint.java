package net.openid.conformance.openbanking_brasil.testmodules.support;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
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

public class CallDirectoryParticipantsEndpoint extends AbstractCondition {

	private static final String PARTICIPANTS_URI = "https://data.sandbox.directory.openbankingbrasil.org.br/participants";

	@Override
	@PostEnvironment(required = "directory_participants_response_full")
	public Environment evaluate(Environment env) {

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
				@Override
				public boolean hasError(ClientHttpResponse response) {
					// Treat all http status codes as 'not an error', so spring never throws an exception due to the http
					// status code meaning the rest of our code can handle http status codes how it likes
					return false;
				}
			});

			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			headers.setAcceptCharset(Collections.singletonList(StandardCharsets.UTF_8));
			HttpMethod httpMethod = HttpMethod.GET;
			HttpEntity<?> request = new HttpEntity<>(headers);

			try {
				ResponseEntity<String> response = restTemplate.exchange(PARTICIPANTS_URI, httpMethod, request, String.class);
				JsonObject responseInfo = convertResponseForEnvironment("directory_participants_uri", response);

				env.putObject("directory_participants_response_full", responseInfo);

				logSuccess("Called directory_participants_uri", responseInfo);

			} catch (RestClientResponseException e) {
				throw error("Error from directory_participants_uri",
					args("directory_participants_uri", PARTICIPANTS_URI, "code", e.getRawStatusCode(),
						"status", e.getStatusText(), "body", e.getResponseBodyAsString()));

			} catch (RestClientException e) {

				String reason = "Unknown";
				if (e.getCause() != null) {
					reason = e.getCause().getMessage();
				}

				throw error("Call to directory_participants_uri failed", e,
					args("directory_participants_uri", PARTICIPANTS_URI, "reason", reason));
			}


		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException |
				 KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}
		return env;
	}
}
