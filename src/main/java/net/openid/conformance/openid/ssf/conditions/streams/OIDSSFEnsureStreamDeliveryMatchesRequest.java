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
 * Checks that the stream the transmitter created carries the delivery the receiver asked for.
 * SSF 1.0 8.1.1.1: the transmitter "MUST process the request as specified" and "MUST include
 * a delivery property in the response with this method property and an endpoint_url
 * property"; CAEP Interop Profile 2.3.8.1: "the Transmitter MUST include a delivery field in
 * the stream configuration ... its method value MUST be one of the delivery methods listed
 * above". The method must be the one sent; for push the endpoint_url is the receiver's and
 * must be echoed, for poll the transmitter supplies it and it must be a non-empty https URL
 * (SSF 1.0 6.1.2, RFC 8936 3: HTTP over TLS).
 * <p>
 * Reads the created stream from {@code ssf.stream} and the request that was sent from
 * {@code ssf.expected_stream_config}; a request that carried no {@code delivery} is covered by
 * {@link OIDSSFEnsureStreamDeliveryDefaultsToPoll} and skipped here.
 */
public class OIDSSFEnsureStreamDeliveryMatchesRequest extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement sentDeliveryEl = env.getElementFromObject("ssf", "expected_stream_config.delivery");
		if (sentDeliveryEl == null || !sentDeliveryEl.isJsonObject()) {
			log("The create request carried no 'delivery' object; the transmitter's default applies and is checked separately");
			return env;
		}
		JsonObject sentDelivery = sentDeliveryEl.getAsJsonObject();
		String sentMethod = OIDFJSON.tryGetString(sentDelivery.get("method"));

		JsonElement streamEl = env.getElementFromObject("ssf", "stream");
		JsonElement deliveryEl = env.getElementFromObject("ssf", "stream.delivery");
		if (deliveryEl == null || !deliveryEl.isJsonObject()) {
			throw error("The stream configuration does not contain a 'delivery' object although the create request asked for one",
				args("sent_delivery", sentDelivery, "stream_configuration", streamEl));
		}
		JsonObject delivery = deliveryEl.getAsJsonObject();
		String method = OIDFJSON.tryGetString(delivery.get("method"));

		if (sentMethod != null && !sentMethod.equals(method)) {
			throw error("The stream configuration carries a different delivery method than the create request asked for. "
					+ "The transmitter must process the request as specified; a delivery method it does not support is refused with 400, not replaced.",
				args("requested_method", sentMethod, "actual_method", delivery.get("method"), "delivery", delivery));
		}

		JsonElement endpointUrlEl = delivery.get("endpoint_url");
		String endpointUrl = OIDFJSON.isString(endpointUrlEl) ? OIDFJSON.getString(endpointUrlEl) : null;

		if (SsfConstants.DELIVERY_METHOD_PUSH_RFC_8935_URI.equals(sentMethod)) {
			String sentEndpointUrl = OIDFJSON.tryGetString(sentDelivery.get("endpoint_url"));
			if (sentEndpointUrl != null && !sentEndpointUrl.equals(endpointUrl)) {
				throw error("The push 'endpoint_url' in the stream configuration is not the one the create request supplied",
					args("requested_endpoint_url", sentEndpointUrl, "actual_endpoint_url", endpointUrlEl, "delivery", delivery));
			}
			logSuccess("The stream configuration carries the requested push delivery", args("delivery", delivery));
			return env;
		}

		if (endpointUrl == null || endpointUrl.isBlank()) {
			throw error("The 'delivery' object of the poll stream does not contain a non-empty 'endpoint_url'. "
					+ "For poll delivery the transmitter supplies the endpoint_url.",
				args("endpoint_url", endpointUrlEl, "delivery", delivery));
		}
		URI endpointUri;
		try {
			endpointUri = new URI(endpointUrl);
		} catch (URISyntaxException e) {
			throw error("The poll 'endpoint_url' of the stream is not a valid URL",
				args("endpoint_url", endpointUrl, "error", e.getMessage(), "delivery", delivery));
		}
		if (!"https".equalsIgnoreCase(endpointUri.getScheme()) || endpointUri.getHost() == null) {
			throw error("The poll 'endpoint_url' of the stream is not an https URL",
				args("endpoint_url", endpointUrl, "delivery", delivery));
		}

		logSuccess("The stream configuration carries the requested poll delivery with an https endpoint_url",
			args("delivery", delivery));
		return env;
	}
}
