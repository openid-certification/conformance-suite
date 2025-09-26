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

		if (!unknownProperties.isEmpty()) {
			log("Found unknown properties in stream request body", args("stream_config", streamConfigInput, "unknown_properties", unknownProperties));
		}
	}

	protected void checkInvalidTransmitterSuppliedProperties(JsonObject streamConfigInput) {
		Set<String> invalidProps = new HashSet<>();
		for (String prop : getTransmitterSuppliedProperties()) {
			if (streamConfigInput.has(prop)) {
				invalidProps.add(prop);
			}
		}
		if (!invalidProps.isEmpty()) {
			throw error("Found transmitter supplied properties in stream request body", args("invalid_transmitter_supplied", invalidProps));
		}
	}

	protected Set<String> getTransmitterSuppliedProperties() {
		return transmitterSuppliedProperties;
	}

	protected void checkDelivery(JsonObject streamConfigInput) {

		JsonObject delivery = streamConfigInput.getAsJsonObject("delivery");
		if (delivery == null) {
			log("No delivery found in stream request, assuming urn:ietf:rfc:8936 (POLL delivery)", args("stream_config", streamConfigInput));
			return;
		}

		String deliveryMethod = OIDFJSON.getString(delivery.get("method"));
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
		JsonArray eventsRequested = streamConfigInput.getAsJsonArray("events_requested");
		if (eventsRequested != null && eventsRequested.isEmpty()) {
			log("Found empty events_requested in stream config");
		} else {
			log("Found events_requested in stream config", args("events_requested", eventsRequested));
		}
	}
}
