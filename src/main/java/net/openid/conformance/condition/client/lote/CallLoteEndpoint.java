package net.openid.conformance.condition.client.lote;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.federation.IgnoreErrorsErrorHandler;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

/**
 * Fetches the list of trusted entities from its registered URL (a distribution point, ETSI
 * TS 119 602 clause 6.3.16) with a GET request and stores the body, a compact JWS, as the list's
 * value, plus the raw response details.
 */
public class CallLoteEndpoint extends AbstractLoteCondition {

	@Override
	@PreEnvironment(required = LOTE)
	@PostEnvironment(required = LOTE)
	public Environment evaluate(Environment env) {
		LoteSlot slot = getSlot(env);
		String url = env.getString(LOTE, "url");

		RestTemplate restTemplate;
		try {
			restTemplate = createRestTemplate(env);
		} catch (Exception e) {
			throw error("Failed to create an HTTP client to fetch the list of trusted entities with", e);
		}
		// treat all http status codes as 'not an error' so the status code check below runs
		restTemplate.setErrorHandler(new IgnoreErrorsErrorHandler());

		ResponseEntity<String> response;
		try {
			HttpHeaders headers = new HttpHeaders();
			// the EU lists are published as application/jwt; the profiles say nothing about the media type
			headers.setAccept(java.util.List.of(MediaType.parseMediaType("application/jwt"), MediaType.ALL));
			response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
		} catch (Exception e) {
			throw error("Unable to retrieve the list of trusted entities from the " + slot.fieldDescription(), e,
				args("url", url));
		}

		env.putObject(LOTE, "endpoint_response", convertResponseForEnvironment("LoTE", response));

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw error("Unable to retrieve the list of trusted entities from the " + slot.fieldDescription()
					+ ": unexpected HTTP status code",
				args("url", url, "http_status", response.getStatusCode().value()));
		}

		String body = response.getBody();
		if (body == null || body.isBlank()) {
			throw error("The list of trusted entities endpoint returned an empty response body", args("url", url));
		}

		env.putString(LOTE, "value", body.trim());
		logSuccess("Retrieved the list of trusted entities from the configured URL",
			args("url", url, "http_status", response.getStatusCode().value(), "body_size", body.length()));
		return env;
	}
}
