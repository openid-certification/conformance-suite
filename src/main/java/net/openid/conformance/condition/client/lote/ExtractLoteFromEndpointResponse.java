package net.openid.conformance.condition.client.lote;

import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;

/**
 * Stores the body of the list of trusted entities endpoint's response, a compact JWS, as the
 * list's value. A response with a status other than 200 stores nothing, so the list is reported
 * unusable once, by the status check, rather than again by every check of its content.
 */
public class ExtractLoteFromEndpointResponse extends AbstractLoteCondition {

	@Override
	@PreEnvironment(required = { LOTE, CallLoteEndpoint.RESPONSE })
	@PostEnvironment(required = LOTE)
	public Environment evaluate(Environment env) {
		Integer status = env.getInteger(CallLoteEndpoint.RESPONSE, "status");
		if (status != null && status != 200) {
			// EnsureHttpStatusCodeIs200 has reported it; an error body is no list, so nothing is parsed
			log("Not using the response of the list of trusted entities endpoint, whose status was " + status,
				args("url", env.getString(LOTE, "url")));
			return env;
		}
		String body = env.getString(CallLoteEndpoint.RESPONSE, "body");
		if (body == null || body.isBlank()) {
			throw error("The list of trusted entities endpoint returned an empty response body", args("url", env.getString(LOTE, "url")));
		}
		env.putString(LOTE, "value", body.trim());
		logSuccess("Retrieved the list of trusted entities from the " + getSlot(env).fieldDescription(),
			args("url", env.getString(LOTE, "url"), "body_size", body.length()));
		return env;
	}
}
