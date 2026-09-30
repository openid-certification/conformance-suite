package net.openid.conformance.condition.client;

import com.google.common.base.Strings;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

public abstract class AbstractRequestUriPostCondition extends AbstractCondition {

	/**
	 * Returns the form parameters the wallet sent in the request_uri POST body.
	 *
	 * All parameters defined in OID4VP 1.0 Final §5.10 are optional, so an empty body is a valid
	 * request with no parameters.
	 */
	protected JsonObject getRequestUriPostFormParams(Environment env) {
		JsonElement formParams = env.getElementFromObject("incoming_request", "body_form_params");
		if (formParams != null) {
			return formParams.getAsJsonObject();
		}

		String body = env.getString("incoming_request", "body");
		if (Strings.isNullOrEmpty(body)) {
			return new JsonObject();
		}

		throw error("The request_uri POST has a body that could not be read as form parameters; the content-type must be application/x-www-form-urlencoded.",
			args("content_type", env.getString("incoming_request", "headers.content-type"),
				"body", body));
	}
}
