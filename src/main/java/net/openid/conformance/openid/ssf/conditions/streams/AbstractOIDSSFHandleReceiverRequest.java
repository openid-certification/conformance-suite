package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class AbstractOIDSSFHandleReceiverRequest extends AbstractCondition {

	protected Set<String> getTransmitterSuppliedStreamConfigKeys() {
		// see: https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-8.1.1
		return Set.of("stream_id", "iss", "aud", "events_supported", "events_delivered", "min_verification_interval", "inactivity_timeout");
	}

	protected Set<String> getReceiverSuppliedStreamConfigKeys() {
		// see: https://openid.github.io/sharedsignals/openid-sharedsignals-framework-1_0.html#section-8.1.1
		return Set.of("events_requested", "description");
	}

	/**
	 * SSF 1.0 8.1.1.1: "If the Transmitter does not support the delivery method, it MAY respond
	 * with HTTP Status Code 400 Bad Request." The emulated transmitter supports only the delivery
	 * method the run was scheduled with ({@code ssf.delivery_methods_supported}, also advertised
	 * in its metadata); when that list is absent every method is accepted. Throws an
	 * {@link IllegalArgumentException} the handlers turn into a 400.
	 */
	protected void ensureDeliveryMethodSupported(Environment env, String deliveryMethod) {
		JsonElement supportedEl = env.getElementFromObject("ssf", "delivery_methods_supported");
		if (supportedEl == null || !supportedEl.isJsonArray()) {
			return;
		}
		List<String> supported = OIDFJSON.convertJsonArrayToList(supportedEl.getAsJsonArray());
		if (!supported.contains(deliveryMethod)) {
			throw new IllegalArgumentException("Delivery method '" + deliveryMethod + "' is not supported by this transmitter; it advertises "
				+ supported + " in delivery_methods_supported. The emulated transmitter only supports the 'SSF Delivery Mode' this test "
				+ "was scheduled with: schedule the test with the delivery mode the receiver uses, or have the receiver request an advertised method.");
		}
	}

	/**
	 * RFC 8935 2.1: SETs are pushed to "a TLS-enabled HTTP endpoint provided by the SET
	 * Recipient". Throws an {@link IllegalArgumentException} the handlers turn into a 400 when
	 * the push endpoint_url is not an https URL; the grade comes from
	 * {@link OIDSSFEnsurePushDeliveryEndpointUrlIsHttps}.
	 */
	protected void ensurePushEndpointUrlIsHttps(String pushEndpointUrl) {
		String scheme;
		try {
			scheme = URI.create(pushEndpointUrl).getScheme();
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException("The push delivery endpoint_url is not a valid URL: " + pushEndpointUrl, e);
		}
		if (!"https".equalsIgnoreCase(scheme)) {
			throw new IllegalArgumentException("The push delivery endpoint_url must be an https URL; SETs are pushed to a TLS-protected endpoint only: " + pushEndpointUrl);
		}
	}

	protected JsonObject createErrorObj(String errCode, String description) {
		JsonObject error = new JsonObject();
		error.addProperty("err", errCode);
		error.addProperty("description", description);
		return error;
	}

	protected JsonObject getOrCreateStreamsObject(Environment env) {

		JsonObject streamsObj;
		JsonElement streamsEl = env.getElementFromObject("ssf", "streams");
		if (streamsEl != null) {
			streamsObj = streamsEl.getAsJsonObject();
		} else {
			streamsObj = new JsonObject();
			env.putObject("ssf", "streams", streamsObj);
		}

		return streamsObj;
	}

	protected JsonObject copyConfigObjectWithoutInternalFields(JsonObject configObject) {
		JsonObject configResult = configObject.deepCopy();
		for (String key : configObject.keySet()) {
			if (key.startsWith("_")) {
				// remove internal fields, e.g. _status
				configResult.remove(key);
			}
		}
		return configResult;
	}

	protected Set<String> computeEventsDelivered(JsonObject streamConfigInput, JsonObject defaultConfig) {
		List<String> eventsSupported = OIDFJSON.convertJsonArrayToList(defaultConfig.get("events_supported").getAsJsonArray());
		JsonElement eventsRequestedEl = streamConfigInput.get("events_requested");
		if (eventsRequestedEl == null) {
			// SSF 1.0 8.1.1.1: the create request MAY contain events_requested. When the
			// receiver does not constrain the set, this emulated transmitter delivers
			// every event type it supports.
			return new LinkedHashSet<>(eventsSupported);
		}
		if (!eventsRequestedEl.isJsonArray()) {
			throw error("events_requested must be a JSON array", args("events_requested", eventsRequestedEl));
		}
		Set<String> eventsDelivered = new LinkedHashSet<>(OIDFJSON.convertJsonArrayToList(eventsRequestedEl.getAsJsonArray()));
		eventsDelivered.retainAll(eventsSupported);
		return eventsDelivered;
	}

	/**
	 * SSF 1.0 §8.1.1.3 / §8.1.1.4: in update (PATCH) and replace (PUT) requests the
	 * "Transmitter-Supplied properties besides the stream_id MAY be present, but
	 * they MUST match the expected value" — on mismatch the transmitter MUST
	 * respond with 400. Returns the transmitter-supplied keys present in the
	 * request body whose values differ from the stored stream configuration.
	 */
	protected Set<String> computeMismatchedTransmitterSuppliedProperties(JsonObject streamConfigInput, JsonObject storedStreamConfig) {
		Set<String> mismatched = new HashSet<>();
		for (String key : getTransmitterSuppliedStreamConfigKeys()) {
			if ("stream_id".equals(key) || !streamConfigInput.has(key)) {
				continue;
			}
			if (!streamConfigInput.get(key).equals(storedStreamConfig.get(key))) {
				mismatched.add(key);
			}
		}
		return mismatched;
	}

	/**
	 * The transmitter-supplied stream configuration keys (other than {@code stream_id}) that a
	 * receiver put into its request body. The transmitter decides those values, so the caller
	 * ignores them.
	 */
	protected Set<String> findTransmitterSuppliedKeysInStreamConfigInput(JsonObject streamConfigInput) {
		Set<String> transmitterSuppliedKeys = new HashSet<>(getTransmitterSuppliedStreamConfigKeys());
		transmitterSuppliedKeys.remove("stream_id");
		transmitterSuppliedKeys.retainAll(streamConfigInput.keySet());
		return transmitterSuppliedKeys;
	}
}
