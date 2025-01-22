package net.openid.conformance.condition.client;

import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResponseErrorHandler;
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
import java.util.List;

/**
 * General utility class for calling endpoints and returning the full response
 */
public abstract class AbstractCallEndpointWithGet extends AbstractCallEndpoint {

	protected Environment callEndpointWithGet(Environment env, ResponseErrorHandler errorHandler, List<MediaType> acceptHeader, String endpointUri, String endpointName, String responseEnvironmentKey) {
		this.endpointName = endpointUri;
		this.responseEnvironmentKey = responseEnvironmentKey;

		try {
			RestTemplate restTemplate = createRestTemplate(env);

			if (null != errorHandler) {
				restTemplate.setErrorHandler(errorHandler);
			}

			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(acceptHeader);

			HttpEntity<?> entity = new HttpEntity<>(headers);

			try {

				ResponseEntity<String> response = restTemplate.exchange(endpointUri, HttpMethod.GET, entity, String.class);
				addFullResponse(env, response);

			} catch (RestClientResponseException e) {
				return handleRestClientResponseException(env, e);
			} catch (RestClientException e) {
				return handleClientException(env, e);
			}

			logSuccess("Got " + endpointName + " response", env.getObject(responseEnvironmentKey));
			return env;
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException |
				 NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP Client", e);
		}
	}
}
