package net.openid.conformance.vci10issuer.condition;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

/**
 * Retrieves an SD-JWT VC Type Metadata document via HTTP GET, per
 * IETF SD-JWT VC draft-13 §6.3.1: "The Type Metadata is retrieved using the
 * HTTP GET method. The response MUST be a JSON object as defined in Section
 * 6.2."
 *
 * Stores the raw response under
 * {@code vci.sdjwt_vc_type_metadata_endpoint_response} and the parsed JSON
 * object under {@code vci.sdjwt_vc_type_metadata}. Throws on network errors,
 * non-2xx responses, unparseable bodies, or bodies that are not JSON objects.
 *
 * <p>Treating an HTTPS {@code vct} whose Type Metadata cannot be retrieved as a
 * conformance error (the caller wires this at FAILURE) rests on reading
 * §6.3.1's "Type Metadata MAY be retrieved from it" as "the metadata MUST be
 * available at the URL; only the consumer's use of it is optional", rather than
 * "retrieval may or may not succeed". That interpretation is being confirmed
 * with the working group in
 * <a href="https://github.com/oauth-wg/oauth-sd-jwt-vc/issues/414">oauth-sd-jwt-vc#414</a>;
 * if the WG settles on the weaker reading, the caller should downgrade
 * fetch/transport failures to a WARNING.
 */
public class VCIFetchSdJwtVcTypeMetadata extends AbstractCondition {

	@Override
	@PreEnvironment(required = "vci")
	@PostEnvironment(required = "vci")
	public Environment evaluate(Environment env) {
		String url = env.getString("vci", "sdjwt_vc_type_metadata_url");
		if (url == null || url.isEmpty()) {
			throw error("vci.sdjwt_vc_type_metadata_url is not set; the caller should gate this condition with skipIfMissing");
		}

		ResponseEntity<String> response;
		try {
			RestTemplate restTemplate = createRestTemplate(env);
			MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
			headers.add(HttpHeaders.ACCEPT, "application/json");
			HttpEntity<?> requestEntity = new HttpEntity<>(headers);
			response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
		} catch (UnrecoverableKeyException | KeyManagementException | CertificateException
				 | InvalidKeySpecException | NoSuchAlgorithmException | KeyStoreException | IOException e) {
			throw error("Error creating HTTP client to fetch Type Metadata", e, args("url", url));
		} catch (RestClientException e) {
			String msg = "Unable to fetch SD-JWT VC Type Metadata from " + url;
			if (e.getCause() != null) {
				msg += " - " + e.getCause().getMessage();
			}
			throw error(msg, e, args("url", url));
		}

		JsonObject responseInfo = convertResponseForEnvironment("sdjwt-vc-type-metadata", response);
		env.putObject("vci", "sdjwt_vc_type_metadata_endpoint_response", responseInfo);

		int status = response.getStatusCode().value();
		if (status != 200) {
			throw error("Fetching SD-JWT VC Type Metadata returned a non-200 HTTP status",
				args("url", url, "status", status));
		}

		String body = response.getBody();
		if (body == null || body.isEmpty()) {
			throw error("SD-JWT VC Type Metadata response body is empty; per §6.3.1 the response MUST be a JSON object",
				args("url", url));
		}

		JsonElement parsed;
		try {
			parsed = JsonParser.parseString(body);
		} catch (JsonSyntaxException e) {
			throw error("SD-JWT VC Type Metadata response body is not valid JSON; per §6.3.1 the response MUST be a JSON object",
				e, args("url", url, "body", body));
		}
		if (!parsed.isJsonObject()) {
			throw error("SD-JWT VC Type Metadata response body is not a JSON object; per §6.3.1 the response MUST be a JSON object",
				args("url", url, "body", body));
		}

		JsonObject typeMetadata = parsed.getAsJsonObject();
		env.putObject("vci", "sdjwt_vc_type_metadata", typeMetadata);

		logSuccess("Fetched SD-JWT VC Type Metadata", args("url", url, "type_metadata", typeMetadata));
		return env;
	}
}
