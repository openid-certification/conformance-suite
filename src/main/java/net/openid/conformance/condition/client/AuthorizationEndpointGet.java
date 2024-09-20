package net.openid.conformance.condition.client;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;

public class AuthorizationEndpointGet extends AbstractCondition {

	@Override
	@PreEnvironment(strings = "redirect_to_authorization_endpoint")
	public Environment evaluate(Environment env) {

		String url = env.getString("redirect_to_authorization_endpoint");

		try {
			// The url is already encoded, don't encode again.
			DefaultUriBuilderFactory handler = new DefaultUriBuilderFactory();
			handler.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

			// Enable redirection handling.
			RestTemplate restTemplate = createRestTemplate(env, true, false);
			restTemplate.setUriTemplateHandler(handler);

			// Set a 'Chrome' user agent in the header.
			final HttpHeaders headers = new HttpHeaders();
			headers.set("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/129.0.0.0 Safari/537.36");

			// Create a new HttpEntity
			final HttpEntity<String> entity = new HttpEntity<>(headers);

			ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

			if (response.getStatusCode().value() >= 400 && response.getStatusCode().value() <= 599) {
				throw error("GET to authorization endpoint failed. Status code ' " + response.getStatusCode().value() + "'");
			}

			logSuccess("GET to authorization endpoint succeeded.");

		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Unable to perform GET from authorization endpoint ";
			if (e.getCause() != null) {
				msg += " - " +e.getCause().getMessage();
			}
			throw error(msg, e);
		}

		return env;
	}
}
