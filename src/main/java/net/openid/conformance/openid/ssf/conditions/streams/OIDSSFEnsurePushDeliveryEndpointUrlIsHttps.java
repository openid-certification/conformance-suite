package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.condition.client.AbstractJsonUriIsValidAndHttps;
import net.openid.conformance.openid.ssf.SsfConstants;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URL;

/**
 * RFC 8935 2.1: the push endpoint is "a TLS-enabled HTTP endpoint provided by the SET
 * Recipient"; CAEP Interop Profile 2.1 requires TLS on the network layer. Checks the
 * {@code delivery.endpoint_url} of a push stream request the receiver sent (the parsed body
 * under {@code ssf.stream_input}); a poll request or a request without a delivery object has
 * nothing to check. The emulated transmitter refuses an http endpoint with 400 on its own; this
 * condition carries the grade.
 */
public class OIDSSFEnsurePushDeliveryEndpointUrlIsHttps extends AbstractJsonUriIsValidAndHttps {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement deliveryEl = env.getElementFromObject("ssf", "stream_input.delivery");
		if (deliveryEl == null || !deliveryEl.isJsonObject()) {
			log("The stream request carries no delivery object, nothing to check");
			return env;
		}
		String method = OIDFJSON.tryGetString(deliveryEl.getAsJsonObject().get("method"));
		if (!SsfConstants.DELIVERY_METHOD_PUSH_RFC_8935_URI.equals(method)) {
			log("The stream request does not ask for push delivery, nothing to check", args("method", deliveryEl.getAsJsonObject().get("method")));
			return env;
		}
		JsonElement endpointUrlEl = deliveryEl.getAsJsonObject().get("endpoint_url");
		if (endpointUrlEl == null) {
			// the request validation reports the missing member
			log("The push delivery carries no endpoint_url, nothing to check");
			return env;
		}

		URL endpointUrl = extractURLOrDie(endpointUrlEl, "delivery.endpoint_url");
		if (!endpointUrl.getProtocol().equals(requiredProtocol)) {
			throw error("The push delivery endpoint_url must use the " + requiredProtocol + " scheme; SETs are pushed to a TLS-protected endpoint only",
				args("required", requiredProtocol, "actual_scheme", endpointUrl.getProtocol(), "actual", endpointUrlEl));
		}

		logSuccess("The push delivery endpoint_url uses the " + requiredProtocol + " scheme", args("actual", endpointUrlEl));
		return env;
	}
}
