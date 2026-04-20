package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.common.AbstractWaitForSpecifiedSeconds;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Waits long enough to respect the {@code min_verification_interval} advertised by
 * the transmitter in the stream configuration (SSF 1.0 8.1.4.1). Reads the last
 * verification trigger timestamp from {@code ssf.last_verification_trigger_at} and
 * the advertised interval from {@code ssf.stream.min_verification_interval}; waits
 * only the remaining time. The wait is capped at {@link #MAX_WAIT_SECONDS} so a
 * transmitter advertising an unreasonably large value cannot stall CI.
 */
public class OIDSSFWaitForMinVerificationInterval extends AbstractWaitForSpecifiedSeconds {

	protected static final long MAX_WAIT_SECONDS = 120;

	@Override
	protected long getExpectedWaitSeconds(Environment env) {
		String lastTriggerAtStr = env.getString("ssf", "last_verification_trigger_at");
		if (lastTriggerAtStr == null) {
			return 0;
		}

		JsonElement minIntervalEl = env.getElementFromObject("ssf", "stream.min_verification_interval");
		if (minIntervalEl == null || !minIntervalEl.isJsonPrimitive()) {
			return 0;
		}

		long minIntervalSeconds;
		try {
			minIntervalSeconds = OIDFJSON.getLong(minIntervalEl);
		} catch (RuntimeException e) {
			return 0;
		}
		if (minIntervalSeconds <= 0) {
			return 0;
		}

		Instant last;
		try {
			last = Instant.parse(lastTriggerAtStr);
		} catch (DateTimeParseException e) {
			return 0;
		}

		long elapsed = Duration.between(last, Instant.now()).toSeconds();
		long remaining = minIntervalSeconds - elapsed;
		if (remaining <= 0) {
			return 0;
		}
		if (remaining > MAX_WAIT_SECONDS) {
			log("min_verification_interval exceeds cap; waiting only capped duration",
				args("min_verification_interval_seconds", minIntervalSeconds,
					"cap_seconds", MAX_WAIT_SECONDS,
					"would_wait_seconds", remaining));
			return MAX_WAIT_SECONDS;
		}
		return remaining;
	}
}
