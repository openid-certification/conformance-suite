package net.openid.conformance.openid.ssf.conditions.events;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.testmodule.Environment;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Verifies that all expected CAEP event types were received.
 * The expected set is typically derived from the stream's {@code events_delivered}
 * field, filtered to CAEP event types.
 */
public class OIDSSFEnsureAllCaepInteropEventsReceived extends AbstractCondition {

	private final Set<String> expectedEventTypes;
	private final Set<String> receivedEventTypes;

	public OIDSSFEnsureAllCaepInteropEventsReceived(Set<String> expectedEventTypes, Set<String> receivedEventTypes) {
		this.expectedEventTypes = expectedEventTypes;
		this.receivedEventTypes = receivedEventTypes;
	}

	@Override
	public Environment evaluate(Environment env) {

		Set<String> missing = new LinkedHashSet<>(expectedEventTypes);
		missing.removeAll(receivedEventTypes);

		if (!missing.isEmpty()) {
			throw error("Not all expected CAEP event types were received",
				args("expected", expectedEventTypes, "received", receivedEventTypes, "missing", missing));
		}

		logSuccess("All expected CAEP event types received",
			args("expected", expectedEventTypes, "received", receivedEventTypes));

		return env;
	}
}
