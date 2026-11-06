package net.openid.conformance.poppy.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

/**
 * Shared GET and JSON-response checks for the conditions that fetch Poppy discovery documents.
 */
abstract class AbstractPoppyFetch extends AbstractCondition {

	/**
	 * GET the URL without following redirects and without throwing on 4xx/5xx statuses.
	 */
	protected ResponseEntity<String> get(Environment env, URI url) {
		try {
			RestTemplate restTemplate = createRestTemplate(env);
			restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
				@Override
				public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
					return false;
				}
			});
			return restTemplate.exchange(url, HttpMethod.GET, null, String.class);
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client", e);
		} catch (RestClientException e) {
			String msg = "Fetching " + url + " failed";
			if (e.getCause() != null) {
				msg += " - " + e.getCause().getMessage();
			}
			throw error(msg, e);
		}
	}

	/**
	 * Requires HTTP 200 and a JSON content type, and returns the body parsed as a JSON object.
	 */
	protected JsonObject requireJsonObject(ResponseEntity<String> response, URI url, String what) {
		int status = response.getStatusCode().value();
		if (status != 200) {
			throw error("The " + what + " endpoint did not return HTTP 200",
				args("url", url.toString(), "status", status, "body", response.getBody()));
		}
		MediaType contentType = response.getHeaders().getContentType();
		if (contentType == null || !MediaType.APPLICATION_JSON.equalsTypeAndSubtype(contentType)) {
			throw error("The " + what + " endpoint did not return a Content-Type of application/json",
				args("url", url.toString(), "content_type", contentType == null ? null : contentType.toString()));
		}
		String body = response.getBody();
		try {
			JsonElement element = JsonParser.parseString(body == null ? "" : body);
			if (!element.isJsonObject()) {
				throw error("The " + what + " response is not a JSON object", args("url", url.toString(), "body", body));
			}
			return element.getAsJsonObject();
		} catch (JsonSyntaxException e) {
			throw error("The " + what + " response is not valid JSON", e, args("url", url.toString(), "body", body));
		}
	}
}
