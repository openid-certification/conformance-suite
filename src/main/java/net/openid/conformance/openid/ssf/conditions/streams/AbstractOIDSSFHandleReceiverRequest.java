package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
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
		List<String> eventsRequested = OIDFJSON.convertJsonArrayToList(streamConfigInput.get("events_requested").getAsJsonArray());
		List<String> eventsSupported = OIDFJSON.convertJsonArrayToList(defaultConfig.get("events_supported").getAsJsonArray());
		Set<String> eventsDelivered = new HashSet<>(eventsRequested);
		eventsDelivered.retainAll(eventsSupported);
		return eventsDelivered;
	}

	protected Set<String> checkForInvalidKeysInStreamConfigInput(JsonObject streamConfigInput) {
		Set<String> transmitterSuppliedKeys = new HashSet<>(getTransmitterSuppliedStreamConfigKeys());
		transmitterSuppliedKeys.remove("stream_id"); // ignore stream_id

		Set<String> keysNotAllowedInUpdate = new HashSet<>(transmitterSuppliedKeys);
		keysNotAllowedInUpdate.retainAll(streamConfigInput.keySet());
		return keysNotAllowedInUpdate;
	}
}
