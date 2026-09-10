package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.conditions.streams.OIDSSFHandlePollRequest;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;

/**
 * Summarizes, in a single log entry, which RFC 8936 2.4 poll request variations the receiver
 * used during the run, as counted by {@link OIDSSFHandlePollRequest}: poll-only,
 * acknowledge-only, combined acknowledge-and-poll, and whether it long-polled or short-polled.
 * Informational only: the RFC lets the receiver choose its variation, so this never fails.
 */
public class OIDSSFLogObservedPollRequestVariations extends AbstractCondition {

	private static final List<String> VARIATIONS = List.of(
		OIDSSFHandlePollRequest.VARIATION_POLL_ONLY,
		OIDSSFHandlePollRequest.VARIATION_ACKNOWLEDGE_ONLY,
		OIDSSFHandlePollRequest.VARIATION_POLL_WITH_ACKNOWLEDGEMENT,
		OIDSSFHandlePollRequest.VARIATION_LONG_POLL,
		OIDSSFHandlePollRequest.VARIATION_SHORT_POLL);

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement variationsEl = env.getElementFromObject("ssf", OIDSSFHandlePollRequest.POLL_REQUEST_VARIATIONS_KEY);
		JsonObject variations = variationsEl != null && variationsEl.isJsonObject() ? variationsEl.getAsJsonObject() : new JsonObject();

		List<String> used = new ArrayList<>();
		List<String> notUsed = new ArrayList<>();
		for (String variation : VARIATIONS) {
			int count = variations.has(variation) ? OIDFJSON.getInt(variations.get(variation)) : 0;
			if (count > 0) {
				used.add(variation + " (" + count + ")");
			} else {
				notUsed.add(variation);
			}
		}

		if (used.isEmpty()) {
			log("No poll requests were recorded, so no poll request variation was observed");
			return env;
		}

		logSuccess("Poll request variations used by the receiver: " + String.join(", ", used)
				+ (notUsed.isEmpty() ? "" : "; not used: " + String.join(", ", notUsed))
				+ ". A receiver is free to choose between poll-only, acknowledge-only and combined acknowledge-and-poll requests, and between long and short polls.",
			args("poll_request_variations", variations));
		return env;
	}
}
