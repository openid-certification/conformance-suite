package net.openid.conformance.condition.as.dynregistration;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * request_uris
 * OPTIONAL. Array of request_uri values that are pre-registered by the RP for use at the OP.
 * Servers MAY cache the contents of the files referenced by these URIs and not retrieve them
 * at the time they are used in a request. OPs can require that request_uri values used be
 * pre-registered with the require_request_uri_registration discovery parameter.
 * If the contents of the request file could ever change, these URI values SHOULD include the
 * base64url encoded SHA-256 hash value of the file contents referenced by the URI as the value
 * of the URI fragment. If the fragment value used for a URI changes, that signals the server
 * that its cached value for that URI with the old fragment value is no longer valid.
 *
 */
public class ValidateRequestUris extends AbstractClientValidationCondition
{

	@Override
	@PreEnvironment(required = { "client"})
	public Environment evaluate(Environment env) {
		this.client = env.getObject("client");
		try {
			JsonArray requestUris = getRequestUris();
			if(requestUris==null) {
				logSuccess("request_uris is not set");
				return env;
			}

			for(JsonElement element : requestUris) {
				try {
					String uriString = OIDFJSON.getString(element);
					@SuppressWarnings("unused")
					URI uri = new URI(uriString);
				} catch (OIDFJSON.UnexpectedJsonTypeException unexpectedTypeEx) {
					throw error("request_uris contains a value that is not encoded as a string",
								args("element", element));
				} catch (URISyntaxException syntaxEx) {
					throw error("request_uris contains a value that is not a valid URI",
						args("element", element));
				}
			}
			logSuccess("request_uris is valid", args("request_uris", requestUris));
			return env;

		} catch (IllegalStateException ex) {
			throw error("request_uris is not encoded as a json array",
						args("request_uris", client.get("request_uris")));
		}
	}
}
