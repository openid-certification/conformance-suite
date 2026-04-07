package net.openid.conformance.openid.ssf.conditions.events;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Verifies that all three CAEP Interop Profile event types were received:
 * session-revoked, credential-change, and device-compliance-change.
 */
public class OIDSSFEnsureAllCaepInteropEventsReceived extends AbstractCondition {

	private final Set<String> receivedEventTypes;

	public OIDSSFEnsureAllCaepInteropEventsReceived(Set<String> receivedEventTypes) {
		this.receivedEventTypes = receivedEventTypes;
	}

	@Override
	public Environment evaluate(Environment env) {

		Set<String> expected = SsfEvents.CAEP_INTEROP_EVENT_TYPES;
		Set<String> missing = new LinkedHashSet<>(expected);
		missing.removeAll(receivedEventTypes);

		if (!missing.isEmpty()) {
			throw error("Not all expected CAEP Interop event types were received",
				args("expected", expected, "received", receivedEventTypes, "missing", missing));
		}

		logSuccess("All expected CAEP Interop event types received",
			args("received", receivedEventTypes));

		return env;
	}
}
