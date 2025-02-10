package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class OIDSSFWaitForSetAcknowledgment extends AbstractCondition {

	@Override
	public Environment evaluate(Environment env) {

		JsonObject streamFeedback = null;

		for (int i = 0; i < 12; i++) {
			var streamFeedbackElement = env.getElementFromObject("ssf", "stream.feedback");
			if (streamFeedbackElement != null) {
				streamFeedback = streamFeedbackElement.getAsJsonObject();
				break;
			}

			log("Waiting for stream feedback with SETs for acknowledgment");
			env.doWithLock(() -> {
				if (env.awaitLockCondition("pollingRequestProcessed", 10, TimeUnit.SECONDS)) {
					log("Detected polling request");
				} else {
					log("Woke up after timeout waiting for polling request");
				}
			});
		}

		if (streamFeedback == null) {
			throw error("Stream failed to detect stream feedback.");
		}

		for (int i = 0; i < 12; i++) {
			var streamSets = env.getElementFromObject("ssf", "stream.sets").getAsJsonObject();
			JsonElement acksElement = env.getElementFromObject("ssf", "stream.feedback.acks");
			if (acksElement != null) {
				Set<String> ackedJtis = new HashSet<>();
				JsonArray acks = acksElement.getAsJsonArray();
				for (JsonElement ack : acks) {
					String ackedJti = OIDFJSON.getString(ack);
					if (streamSets.has(ackedJti)) {
						streamSets.remove(ackedJti);
						ackedJtis.add(ackedJti);
					}
				}

				if (!ackedJtis.isEmpty()) {
					logSuccess("Detected acknowledgment for SETs", args("JTIs", ackedJtis));
					return env;
				} else {
					log("Failed to detect acknowledgment for SETs", args("expected_jtis", streamSets.keySet(), "received_jtis", ackedJtis));

					env.doWithLock(() -> {
						log("Waiting for polling request");
						if (env.awaitLockCondition("pollingRequestProcessed", 10, TimeUnit.SECONDS)) {
							log("Detected polling request");
						} else {
							log("Woke up after timeout waiting for polling request");
						}
					});
				}
			}
		}

		throw error("Stream failed to detect acknowledgment for SETs");
	}
}
