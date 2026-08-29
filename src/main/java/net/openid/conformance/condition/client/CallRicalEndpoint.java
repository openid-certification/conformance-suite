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
 * Fetches the signed RICAL from the configured RICAL URL with a GET request (ISO/IEC 18013-5
 * second edition draft Annex F.3.2.1 describes RICAL endpoints as accepting GET requests and
 * returning a CBOR payload) and stores it in the environment the same way as an inline
 * configured RICAL, plus the raw response details for downstream response checks.
 */
public class CallRicalEndpoint extends AbstractCondition {

	@Override
	@PreEnvironment(strings = { "rical_url" })
	@PostEnvironment(required = { "rical", "rical_endpoint_response" })
	public Environment evaluate(Environment env) {

		String ricalUrl = env.getString("rical_url");

		RestTemplate restTemplate;
		try {
			restTemplate = createRestTemplate(env);
		} catch (Exception e) {
			throw error("Failed to create an HTTP client to fetch the RICAL with", e);
		}
		// treat all http status codes as 'not an error' so the status code check below runs
		restTemplate.setErrorHandler(new IgnoreErrorsErrorHandler());

		ResponseEntity<byte[]> response;
		try {
			// no Accept header, and deliberately no response content-type check: Annex F says
			// application/cbor for latestRicalUrl endpoints, but the configured distribution
			// URL is not necessarily such an endpoint (real deployments serve
			// application/octet-stream) and the draft may yet change - revisit once the
			// second edition is published
			HttpHeaders headers = new HttpHeaders();
			response = restTemplate.exchange(ricalUrl, HttpMethod.GET, new HttpEntity<>(headers), byte[].class);
		} catch (Exception e) {
			throw error("Unable to retrieve the RICAL from the 'RICAL URL' given in the test configuration", e,
				args("rical_url", ricalUrl));
		}

		env.putObject("rical_endpoint_response", convertBinaryResponseForEnvironment("RICAL", response));

		if (!response.getStatusCode().is2xxSuccessful()) {
			throw error("Unable to retrieve the RICAL from the 'RICAL URL' given in the test configuration: unexpected HTTP status code",
				args("rical_url", ricalUrl,
					"http_status", response.getStatusCode().value()));
		}

		byte[] body = response.getBody();
		if (body == null || body.length == 0) {
			throw error("The RICAL endpoint returned an empty response body",
				args("rical_url", ricalUrl));
		}

		JsonObject rical = new JsonObject();
		rical.addProperty("value", Base64.getEncoder().encodeToString(body));
		env.putObject("rical", rical);

		logSuccess("Retrieved RICAL from the configured URL",
			args("rical_url", ricalUrl,
				"http_status", response.getStatusCode().value(),
				"body_size", body.length));

		return env;
	}
}
