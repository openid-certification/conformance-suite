package net.openid.conformance.poppy.condition;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.nimbusds.jose.jwk.JWKSet;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.text.ParseException;

/**
 * Fetches the Personal Agent's key set from {@code client.jwks_uri} without following redirects,
 * checking status, content type and that the body is a JWK set.
 */
public class FetchPoppyClientJwks extends AbstractPoppyFetch {

	private static final MediaType JWK_SET_JSON = MediaType.parseMediaType("application/jwk-set+json");

	@Override
	@PreEnvironment(required = "client")
	@PostEnvironment(required = "client")
	public Environment evaluate(Environment env) {
		String jwksUri = env.getString("client", "jwks_uri");
		if (jwksUri == null || jwksUri.isBlank()) {
			throw error("The client metadata document has no jwks_uri");
		}
		URI url = URI.create(jwksUri);
		ResponseEntity<String> response = get(env, url);
		int status = response.getStatusCode().value();
		if (status != 200) {
			throw error("The Personal Agent's jwks_uri did not return HTTP 200 (redirects are not followed)",
				args("url", jwksUri, "status", status, "body", response.getBody()));
		}
		MediaType contentType = response.getHeaders().getContentType();
		if (contentType == null || !(MediaType.APPLICATION_JSON.equalsTypeAndSubtype(contentType) || JWK_SET_JSON.equalsTypeAndSubtype(contentType))) {
			throw error("The Personal Agent's jwks_uri did not return a Content-Type of application/json or application/jwk-set+json",
				args("url", jwksUri, "content_type", contentType == null ? null : contentType.toString()));
		}
		String body = response.getBody() == null ? "" : response.getBody();
		JsonObject jwks;
		try {
			JsonElement element = JsonParser.parseString(body);
			JsonElement keys = element.isJsonObject() ? element.getAsJsonObject().get("keys") : null;
			if (keys == null || !keys.isJsonArray() || !allObjects(keys.getAsJsonArray())) {
				throw error("The Personal Agent's jwks_uri response is not a JWK set: it needs a 'keys' array of JWK objects",
					args("url", jwksUri, "body", body));
			}
			jwks = element.getAsJsonObject();
			JWKSet.parse(body);
		} catch (JsonSyntaxException e) {
			throw error("The Personal Agent's jwks_uri response is not valid JSON", e, args("url", jwksUri, "body", body));
		} catch (ParseException e) {
			throw error("The Personal Agent's jwks_uri response is not a valid JWK set", e, args("url", jwksUri, "body", body));
		}
		env.getObject("client").add("jwks", jwks);
		logSuccess("Fetched the Personal Agent's key set", args("url", jwksUri, "jwks", jwks));
		return env;
	}

	private static boolean allObjects(JsonArray keys) {
		for (JsonElement key : keys) {
			if (!key.isJsonObject()) {
				return false;
			}
		}
		return true;
	}
}
