package net.openid.conformance.openid.ssf.conditions.streams;

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

/**
 * Compares the stream configuration the transmitter returned or serves ({@code ssf.stream})
 * with the receiver-supplied properties the suite sent in its last PATCH or PUT
 * ({@code ssf.expected_stream_config}).
 * <p>
 * SSF 1.0 8.1.1.3 (PATCH): "Any Receiver-Supplied property present in the request MUST be
 * updated by the Transmitter. Any properties missing in the request MUST NOT be changed."
 * SSF 1.0 8.1.1.4 (PUT): "Missing Receiver-Supplied properties MUST be interpreted as
 * requested to be deleted." A deleted {@code delivery} falls back to the transmitter default,
 * poll (8.1.1.1). In both cases {@code events_delivered} stays a subset of the
 * {@code events_requested} that was sent (8.1.1).
 */
public class OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties extends AbstractCondition {

	public enum Operation {
		/** PATCH: omitted properties keep their value */
		UPDATE,
		/** PUT: omitted properties are deleted */
		REPLACE
	}

	private final Operation operation;

	public OIDSSFEnsureStreamConfigReflectsReceiverSuppliedProperties(Operation operation) {
		this.operation = operation;
	}

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement sentEl = env.getElementFromObject("ssf", "expected_stream_config");
		JsonElement actualEl = env.getElementFromObject("ssf", "stream");
		if (sentEl == null || !sentEl.isJsonObject() || actualEl == null || !actualEl.isJsonObject()) {
			throw error("Could not find the sent request body or the returned stream configuration to compare",
				args("sent", sentEl, "stream", actualEl));
		}
		JsonObject sent = sentEl.getAsJsonObject();
		JsonObject actual = actualEl.getAsJsonObject();

		Set<String> mismatches = computeMismatches(sent, actual, operation);

		if (!mismatches.isEmpty()) {
			throw error("The stream configuration does not reflect the receiver-supplied properties of the " + operation.name().toLowerCase() + " request",
				args("operation", operation.name(), "mismatches", mismatches, "sent", sent, "stream", actual));
		}

		logSuccess("The stream configuration reflects the receiver-supplied properties of the " + operation.name().toLowerCase() + " request",
			args("operation", operation.name(), "sent", sent, "stream", actual));
		return env;
	}

	/**
	 * The receiver-supplied properties of {@code sent} that {@code actual} does not reflect;
	 * empty when the stream configuration matches. Modules that received a 202 use this to
	 * wait for the transmitter to finish processing before running the graded check.
	 */
	public static Set<String> computeMismatches(JsonObject sent, JsonObject actual, Operation operation) {
		Set<String> mismatches = new HashSet<>();
		checkDescription(sent, actual, mismatches, operation);
		checkEventsRequested(sent, actual, mismatches, operation);
		checkDelivery(sent, actual, mismatches, operation);
		return mismatches;
	}

	private static void checkDescription(JsonObject sent, JsonObject actual, Set<String> mismatches, Operation operation) {
		if (sent.has("description")) {
			if (!sent.get("description").equals(actual.get("description"))) {
				mismatches.add("description: sent " + sent.get("description") + " but the stream has " + actual.get("description"));
			}
		} else if (operation == Operation.REPLACE && actual.has("description")) {
			mismatches.add("description: omitted from the PUT body, so it must be deleted, but the stream still has " + actual.get("description"));
		}
	}

	private static void checkEventsRequested(JsonObject sent, JsonObject actual, Set<String> mismatches, Operation operation) {
		JsonElement sentEventsEl = sent.get("events_requested");
		if (sentEventsEl != null && sentEventsEl.isJsonArray()) {
			Set<String> sentEvents = new HashSet<>(OIDFJSON.convertJsonArrayToList(sentEventsEl.getAsJsonArray()));
			JsonElement actualEventsEl = actual.get("events_requested");
			Set<String> actualEvents = actualEventsEl != null && actualEventsEl.isJsonArray()
				? new HashSet<>(OIDFJSON.convertJsonArrayToList(actualEventsEl.getAsJsonArray())) : Set.of();
			if (!sentEvents.equals(actualEvents)) {
				mismatches.add("events_requested: sent " + sentEvents + " but the stream has " + actualEvents);
			}
			JsonElement deliveredEl = actual.get("events_delivered");
			if (deliveredEl != null && deliveredEl.isJsonArray()) {
				Set<String> delivered = new HashSet<>(OIDFJSON.convertJsonArrayToList(deliveredEl.getAsJsonArray()));
				delivered.removeAll(sentEvents);
				if (!delivered.isEmpty()) {
					mismatches.add("events_delivered contains event types not in the sent events_requested: " + delivered);
				}
			}
		} else if (operation == Operation.REPLACE) {
			JsonElement actualEventsEl = actual.get("events_requested");
			if (actualEventsEl != null && actualEventsEl.isJsonArray() && !actualEventsEl.getAsJsonArray().isEmpty()) {
				mismatches.add("events_requested: omitted from the PUT body, so it must be deleted, but the stream still has " + actualEventsEl);
			}
		}
	}

	private static void checkDelivery(JsonObject sent, JsonObject actual, Set<String> mismatches, Operation operation) {
		JsonElement actualDeliveryEl = actual.get("delivery");
		JsonObject actualDelivery = actualDeliveryEl != null && actualDeliveryEl.isJsonObject() ? actualDeliveryEl.getAsJsonObject() : new JsonObject();
		String actualMethod = OIDFJSON.tryGetString(actualDelivery.get("method"));

		JsonElement sentDeliveryEl = sent.get("delivery");
		if (sentDeliveryEl != null && sentDeliveryEl.isJsonObject()) {
			JsonObject sentDelivery = sentDeliveryEl.getAsJsonObject();
			String sentMethod = OIDFJSON.tryGetString(sentDelivery.get("method"));
			if (sentMethod != null && !sentMethod.equals(actualMethod)) {
				mismatches.add("delivery.method: sent " + sentMethod + " but the stream has " + actualMethod);
			}
			// the push endpoint is receiver-supplied; the poll endpoint is transmitter-supplied
			if (DELIVERY_METHOD_PUSH_RFC_8935_URI.equals(sentMethod) && sentDelivery.has("endpoint_url")
				&& !sentDelivery.get("endpoint_url").equals(actualDelivery.get("endpoint_url"))) {
				mismatches.add("delivery.endpoint_url: sent " + sentDelivery.get("endpoint_url") + " but the stream has " + actualDelivery.get("endpoint_url"));
			}
		} else if (operation == Operation.REPLACE && !DELIVERY_METHOD_POLL_RFC_8936_URI.equals(actualMethod)) {
			mismatches.add("delivery: omitted from the PUT body, so the transmitter default poll applies, but the stream has " + actualMethod);
		}
	}
}
