package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfConstants;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * SSF 1.0 8.1.1.1: "If the request does not contain the delivery property, then the
 * Transmitter MUST assume that the method is urn:ietf:rfc:8936 (poll). If the Transmitter
 * supports Poll-Based Delivery, the Transmitter MUST include a delivery property in the
 * response with this method property and an endpoint_url property."
 * <p>
 * Checks the stream configuration returned for a create request that carried no
 * {@code delivery} member (at {@code ssf.stream}): the {@code delivery} object must be
 * present, name the poll method and carry a non-empty https {@code endpoint_url}.
 */
public class OIDSSFEnsureStreamDeliveryDefaultsToPoll extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement streamEl = env.getElementFromObject("ssf", "stream");
		JsonElement deliveryEl = env.getElementFromObject("ssf", "stream.delivery");
		if (deliveryEl == null || !deliveryEl.isJsonObject()) {
			throw error("The stream configuration returned for a create request without a 'delivery' property does not contain a 'delivery' object. "
					+ "The transmitter must assume poll delivery and return the delivery method and endpoint_url.",
				args("stream_configuration", streamEl));
		}

		JsonObject delivery = deliveryEl.getAsJsonObject();

		JsonElement methodEl = delivery.get("method");
		String method = OIDFJSON.isString(methodEl) ? OIDFJSON.getString(methodEl) : null;
		if (!SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI.equals(method)) {
			throw error("The stream configuration returned for a create request without a 'delivery' property does not default to poll delivery",
				args("expected_method", SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI, "actual_method", delivery.get("method"), "delivery", delivery));
		}

		JsonElement endpointUrlEl = delivery.get("endpoint_url");
		String endpointUrl = OIDFJSON.isString(endpointUrlEl) ? OIDFJSON.getString(endpointUrlEl) : null;
		if (endpointUrl == null || endpointUrl.isBlank()) {
			throw error("The 'delivery' object of the created poll stream does not contain a non-empty 'endpoint_url'. "
					+ "For poll delivery the transmitter supplies the endpoint_url.",
				args("endpoint_url", endpointUrlEl, "delivery", delivery));
		}

		URI endpointUri;
		try {
			endpointUri = new URI(endpointUrl);
		} catch (URISyntaxException e) {
			throw error("The poll 'endpoint_url' of the created stream is not a valid URL",
				args("endpoint_url", endpointUrl, "error", e.getMessage(), "delivery", delivery));
		}
		if (!"https".equalsIgnoreCase(endpointUri.getScheme()) || endpointUri.getHost() == null) {
			throw error("The poll 'endpoint_url' of the created stream is not an https URL",
				args("endpoint_url", endpointUrl, "delivery", delivery));
		}

		logSuccess("The transmitter defaulted the stream to poll delivery and supplied a poll endpoint_url",
			args("method", method, "endpoint_url", endpointUrl, "delivery", delivery));

		return env;
	}
}
