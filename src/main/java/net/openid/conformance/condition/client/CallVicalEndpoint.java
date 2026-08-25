package net.openid.conformance.condition.client;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.federation.IgnoreErrorsErrorHandler;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

/**
 * Fetches the signed VICAL from the configured VICAL URL with a GET request (ISO/IEC 18013-5
 * Annex C.1.7.3 describes the VICAL endpoint as accepting GET requests and returning the signed
 * VICAL) and stores it in the environment the same way as an inline configured VICAL, plus the
 * raw response details for downstream response checks.
 */
public class CallVicalEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "vical_url" })
	@PostEnvironment(required = { "vical", "vical_endpoint_response" })
	public Environment evaluate(Environment env) {

		String vicalUrl = env.getString("vical_url");

		RestTemplate restTemplate;
		try {
			restTemplate = createRestTemplate(env);
		} catch (Exception e) {
			throw error("Failed to create an HTTP client to fetch the VICAL with", e);
		}
		// treat all http status codes as 'not an error' so the status code check below runs
		restTemplate.setErrorHandler(new IgnoreErrorsErrorHandler());

		ResponseEntity<byte[]> response;
		try {
			// no Accept header: the expected content type is unsettled in the second-edition
			// draft (application/cwt for VICAL vs application/cbor for RICAL endpoints)
			HttpHeaders headers = new HttpHeaders();
			response = restTemplate.exchange(vicalUrl, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
		} catch (Exception e) {
			throw error("Unable to retrieve the VICAL from the 'VICAL URL' given in the test configuration", e,
				args("vical_url", vicalUrl));
		}

		env.putObject("vical_endpoint_response", convertBinaryResponseForEnvironment("VICAL", response));

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw error("Unable to retrieve the VICAL from the 'VICAL URL' given in the test configuration: unexpected HTTP status code",
				args("vical_url", vicalUrl,
					"http_status", response.getStatusCode().value()));
		}

		byte[] body = response.getBody();
		if (body == null || body.length == 0) {
			throw error("The VICAL endpoint returned an empty response body",
				args("vical_url", vicalUrl));
		}

		JsonObject vical = new JsonObject();
		vical.addProperty("value", Base64.getEncoder().encodeToString(body));
		env.putObject("vical", vical);

		logSuccess("Retrieved VICAL from the configured URL",
			args("vical_url", vicalUrl,
				"http_status", response.getStatusCode().value(),
				"body_size", body.length));

		return env;
	}
}
