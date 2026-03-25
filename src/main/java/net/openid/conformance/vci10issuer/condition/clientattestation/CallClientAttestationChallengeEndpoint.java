package net.openid.conformance.vci10issuer.condition.clientattestation;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractCallEndpoint;
import net.openid.conformance.openid.federation.IgnoreErrorsErrorHandler;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

public class CallClientAttestationChallengeEndpoint extends AbstractCallEndpoint {

	@Override
	@PreEnvironment(required = "server")
	@PostEnvironment(required = "challenge_endpoint_response")
	public Environment evaluate(Environment env) {
		final String endpointUri = env.getString("server", "challenge_endpoint");
		this.endpointName = endpointUri;
		this.responseEnvironmentKey = "challenge_endpoint_response";

		try {
			RestTemplate restTemplate = createRestTemplate(env);
			restTemplate.setErrorHandler(new IgnoreErrorsErrorHandler());

			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

			HttpEntity<Void> request = new HttpEntity<>(null, headers);

			try {
				ResponseEntity<String> response = restTemplate.exchange(endpointUri, HttpMethod.POST, request, String.class);
				addFullResponse(env, response);
			} catch (RestClientResponseException e) {
				return handleRestClientResponseException(env, e);
			} catch (RestClientException e) {
				return handleClientException(env, e);
			}

			logSuccess("Got challenge endpoint response", env.getObject(responseEnvironmentKey));
			return env;
		} catch (NoSuchAlgorithmException | KeyManagementException | CertificateException | InvalidKeySpecException |
				 KeyStoreException | IOException | UnrecoverableKeyException e) {
			throw error("Error creating HTTP Client", e);
		}
	}
}
