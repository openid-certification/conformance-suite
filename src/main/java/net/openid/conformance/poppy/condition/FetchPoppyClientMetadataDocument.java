package net.openid.conformance.poppy.condition;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.ResponseEntity;

import java.net.URI;

/**
 * Fetches the Personal Agent's client metadata document from the {@code client_id} in the
 * authorization request.
 */
public class FetchPoppyClientMetadataDocument extends AbstractPoppyFetch {

	@Override
	@PreEnvironment(strings = "poppy_incoming_client_id")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		String clientId = env.getString("poppy_incoming_client_id");
		if (!PoppyUtil.isHttpsUrl(clientId)) {
			throw error("The client_id in the authorization request is not an HTTPS URL, but a Personal Agent's client_id MUST be an HTTPS URL that returns its metadata document",
				args("client_id", clientId));
		}
		URI url = URI.create(clientId);
		ResponseEntity<String> response = get(env, url);
		if (response.getStatusCode().is3xxRedirection()) {
			throw error("The client metadata document URL answered with a redirect, but the Company fetches client metadata without following redirects (CIMD section 5, OAuth Client ID Metadata Document draft; POPPY-5.3)",
				args("url", clientId, "status", response.getStatusCode().value(),
					"location", response.getHeaders().getFirst("Location")));
		}
		JsonObject document = requireJsonObject(response, url, "client metadata document");
		env.putObject("client", document);
		logSuccess("Fetched the client metadata document", args("url", clientId, "client", document));
		return env;
	}
}
