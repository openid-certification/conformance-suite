package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;

import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_POLL_RFC_8936_URI;
import static net.openid.conformance.openid.ssf.SsfConstants.DELIVERY_METHOD_PUSH_RFC_8935_URI;

public class OIDSSFHandleStreamCreateRequestValidation extends AbstractCondition {

	protected Set<String> transmitterSuppliedProperties = Set.of("stream_id", "iss", "aud", "events_supported", "events_delivered", "min_verification_interval", "inactivity_timeout");

	protected Set<String> supportedReceiverSuppliedProperties = Set.of("events_requested", "delivery", "description");

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement streamConfigInputEl = env.getElementFromObject("ssf", "stream_input");
		if (streamConfigInputEl == null) {
			String rawStreamInput = env.getString("ssf", "stream_input_raw");
			throw error("Failed to validate stream request: Stream config missing or invalid",
				args("error", "Could not find stream config in request body", "unparsed_stream_input", rawStreamInput));
		}

		JsonObject streamConfigInput = streamConfigInputEl.getAsJsonObject();

		checkEventsRequested(streamConfigInput);

		checkDelivery(streamConfigInput);

		checkInvalidTransmitterSuppliedProperties(streamConfigInput);

		checkUnknownProperties(streamConfigInput);

		logSuccess("Found valid stream configuration in stream request body", args("stream_config", streamConfigInput));

		return env;
	}

	protected void checkUnknownProperties(JsonObject streamConfigInput) {

		Set<String> unknownProperties = new HashSet<>(streamConfigInput.keySet());
		unknownProperties.removeAll(supportedReceiverSuppliedProperties);
		// transmitter-supplied properties are reported by checkInvalidTransmitterSuppliedProperties
		unknownProperties.removeAll(transmitterSuppliedProperties);

		if (!unknownProperties.isEmpty()) {
			log("Found unknown properties in stream request body. This may indicate the receiver has misunderstood the spec, or it may be using extensions the test suite is unaware of.",
				args("stream_config", streamConfigInput, "unknown_properties", unknownProperties));
		}
	}

	/**
	 * SSF 1.0 8.1.1.1 lists what a Create Stream request MAY contain and forbids nothing;
	 * Table 1 reserves 400 for a request that "cannot be parsed". Transmitter-supplied
	 * properties in the body are therefore not a reason to reject the request: the
	 * transmitter decides those values and ignores the ones sent. Their presence is graded
	 * separately as a sender-side WARNING by
	 * {@link OIDSSFWarnTransmitterSuppliedPropertiesInStreamCreateRequest}.
	 */
	protected void checkInvalidTransmitterSuppliedProperties(JsonObject streamConfigInput) {
		Set<String> transmitterSupplied = new HashSet<>(getTransmitterSuppliedProperties());
		transmitterSupplied.retainAll(streamConfigInput.keySet());
		if (!transmitterSupplied.isEmpty()) {
			log("Found transmitter-supplied properties in the stream create request body; the transmitter's own values are used instead",
				args("transmitter_supplied", transmitterSupplied));
		}
	}

	protected Set<String> getTransmitterSuppliedProperties() {
		return transmitterSuppliedProperties;
	}

	protected void checkDelivery(JsonObject streamConfigInput) {

		JsonElement deliveryEl = streamConfigInput.get("delivery");
		if (deliveryEl == null) {
			log("No delivery found in stream request, assuming urn:ietf:rfc:8936 (POLL delivery)", args("stream_config", streamConfigInput));
			return;
		}
		if (!deliveryEl.isJsonObject()) {
			throw error("delivery must be a JSON object", args("delivery", deliveryEl));
		}
		JsonObject delivery = deliveryEl.getAsJsonObject();

		JsonElement deliveryMethodEl = delivery.get("method");
		if (deliveryMethodEl == null) {
			throw error("Required 'method' property missing from delivery object", args("delivery", delivery));
		}
		String deliveryMethod = OIDFJSON.getString(deliveryMethodEl);
		if (!Set.of(DELIVERY_METHOD_POLL_RFC_8936_URI, DELIVERY_METHOD_PUSH_RFC_8935_URI).contains(deliveryMethod)) {
			throw error("Found unsupported delivery method in stream config", args("delivery_method", deliveryMethod));
		}
		log("Found supported delivery method in stream config", args("delivery_method", deliveryMethod));

		switch (deliveryMethod) {
			case DELIVERY_METHOD_POLL_RFC_8936_URI:
				break;
			case DELIVERY_METHOD_PUSH_RFC_8935_URI:
				JsonElement endpointUrl = delivery.get("endpoint_url");
				if (endpointUrl == null) {
					throw error("Required Delivery endpoint_url missing for RFC-935 Push Delivery", args("delivery_method", deliveryMethod, "delivery", delivery));
				}
				break;
		}
	}

	protected void checkEventsRequested(JsonObject streamConfigInput) {
		JsonElement eventsRequestedEl = streamConfigInput.get("events_requested");
		if (eventsRequestedEl == null) {
			log("No events_requested in stream config, which is optional");
			return;
		}
		if (!eventsRequestedEl.isJsonArray()) {
			throw error("events_requested must be a JSON array", args("events_requested", eventsRequestedEl));
		}
		JsonArray eventsRequested = eventsRequestedEl.getAsJsonArray();
		if (eventsRequested.isEmpty()) {
			log("Found empty events_requested in stream config");
		} else {
			log("Found events_requested in stream config", args("events_requested", eventsRequested));
		}
	}
}
