package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

/**
 * RFC 8935 2.2: a SET recipient acknowledges a push delivery with a 202 whose body
 * "MUST be empty". Reads the receiver's response recorded under {@code endpoint_response}
 * by the push delivery call; a whitespace-only body counts as empty.
 */
public class OIDSSFEnsurePushDeliveryResponseBodyIsEmpty extends AbstractCondition {

	@Override
	@PreEnvironment(required = "endpoint_response")
	public Environment evaluate(Environment env) {

		JsonElement bodyEl = env.getElementFromObject("endpoint_response", "body");
		String body = bodyEl == null || bodyEl.isJsonNull() ? null : OIDFJSON.getString(bodyEl);

		if (body == null || body.isBlank()) {
			logSuccess("The receiver acknowledged the push delivery with an empty response body");
			return env;
		}

		throw error("The receiver's acknowledgement of the push delivery carries a response body; the body of a successful push delivery response must be empty",
			args("status", env.getElementFromObject("endpoint_response", "status"), "body", body));
	}
}
