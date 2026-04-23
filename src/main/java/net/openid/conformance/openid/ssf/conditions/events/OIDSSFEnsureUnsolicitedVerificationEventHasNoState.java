package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Enforces SSF 1.0 8.1.4.2: "If the Verification Event is initiated by the
 * Transmitter then this parameter [state] MUST not be set."
 * <p>
 * Runs against the currently-parsed verification event (at
 * {@code ssf.verification.token.claims}). A verification event is considered
 * transmitter-initiated (unsolicited) when it was received before we sent any
 * verification request — determined by comparing
 * {@code ssf.push_request_received_at} to {@code ssf.last_verification_trigger_at}.
 * If no trigger has been sent yet, every received event is unsolicited.
 * <p>
 * Skipped when the event arrived after our trigger (a legitimate solicited
 * event is allowed, and required, to echo the state we sent), or when the
 * expected environment timestamps are missing.
 */
public class OIDSSFEnsureUnsolicitedVerificationEventHasNoState extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		String pushReceivedAtStr = env.getString("ssf", "push_request_received_at");
		if (pushReceivedAtStr == null) {
			log("Skipping unsolicited-state check: no push_request_received_at available");
			return env;
		}

		Instant pushReceivedAt;
		try {
			pushReceivedAt = Instant.parse(pushReceivedAtStr);
		} catch (DateTimeParseException e) {
			log("Skipping unsolicited-state check: could not parse push_request_received_at",
				args("push_request_received_at", pushReceivedAtStr));
			return env;
		}

		String lastTriggerAtStr = env.getString("ssf", "last_verification_trigger_at");
		boolean preTrigger;
		if (lastTriggerAtStr == null) {
			preTrigger = true;
		} else {
			try {
				Instant lastTriggerAt = Instant.parse(lastTriggerAtStr);
				preTrigger = pushReceivedAt.isBefore(lastTriggerAt);
			} catch (DateTimeParseException e) {
				log("Skipping unsolicited-state check: could not parse last_verification_trigger_at",
					args("last_verification_trigger_at", lastTriggerAtStr));
				return env;
			}
		}

		if (!preTrigger) {
			logSuccess("Verification event arrived after our verification request — 'state' claim is allowed (and required) for solicited events");
			return env;
		}

		JsonElement claimsEl = env.getElementFromObject("ssf", "verification.token.claims");
		if (claimsEl == null || !claimsEl.isJsonObject()) {
			log("Skipping unsolicited-state check: no parsed verification claims available");
			return env;
		}
		JsonObject events = claimsEl.getAsJsonObject().getAsJsonObject("events");
		if (events == null) {
			return env;
		}
		JsonElement verificationEventEl = events.get(SsfEvents.SSF_STREAM_VERIFICATION_EVENT_TYPE);
		if (verificationEventEl == null || !verificationEventEl.isJsonObject()) {
			return env;
		}
		JsonObject verificationEvent = verificationEventEl.getAsJsonObject();

		if (verificationEvent.has("state")) {
			throw error("Transmitter-initiated verification event (received before any verification request was sent) "
					+ "MUST NOT carry a 'state' claim",
				args("push_request_received_at", pushReceivedAtStr,
					"last_verification_trigger_at", lastTriggerAtStr,
					"verification_event", verificationEvent));
		}

		logSuccess("Pre-trigger verification event correctly carries no 'state' claim",
			args("push_request_received_at", pushReceivedAtStr,
				"last_verification_trigger_at", lastTriggerAtStr));
		return env;
	}
}
