package net.openid.conformance.openid.ssf.conditions.streams;

import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class OIDSSFCheckSupportedEventsForStream extends AbstractCondition {

	public static final Set<String> VERIFICATION_EVENT_TYPES = Set.of(
		// see: https://openid.net/specs/openid-sharedsignals-framework-1_0.html#section-7.1.4.1
		"https://schemas.openid.net/secevent/ssf/event-type/verification"
	);

	public static final Set<String> CAEP_EVENT_TYPES = Set.of(
		// See: https://openid.net/specs/openid-caep-1_0-ID2.html#name-event-types
		"https://schemas.openid.net/secevent/caep/event-type/session-revoked", //
		"https://schemas.openid.net/secevent/caep/event-type/token-claims-change", //
		"https://schemas.openid.net/secevent/caep/event-type/credential-change", //
		"https://schemas.openid.net/secevent/caep/event-type/assurance-level-change", //
		"https://schemas.openid.net/secevent/caep/event-type/device-compliance-change", //
		"https://schemas.openid.net/secevent/caep/event-type/session-established", //
		"https://schemas.openid.net/secevent/caep/event-type/session-presented" //
	);


	public static final Set<String> RISC_EVENT_TYPES = Set.of(
		// See: https://openid.net/specs/openid-risc-event-types-1_0-ID1.html
		"https://schemas.openid.net/secevent/risc/event-type/account-credential-change-required",
		"https://schemas.openid.net/secevent/risc/event-type/account-purged",
		"https://schemas.openid.net/secevent/risc/event-type/account-disabled",
		"https://schemas.openid.net/secevent/risc/event-type/account-enabled",
		"https://schemas.openid.net/secevent/risc/event-type/identifier-changed",
		"https://schemas.openid.net/secevent/risc/event-type/identifier-recycled",
		"https://schemas.openid.net/secevent/risc/event-type/opt-in",
		"https://schemas.openid.net/secevent/risc/event-type/opt-out-initiated",
		"https://schemas.openid.net/secevent/risc/event-type/opt-out-cancelled",
		"https://schemas.openid.net/secevent/risc/event-type/opt-out-effective",
		"https://schemas.openid.net/secevent/risc/event-type/recovery-activated",
		"https://schemas.openid.net/secevent/risc/event-type/recovery-information-changed",
		"https://schemas.openid.net/secevent/risc/event-type/sessions-revoked"
	);

	public static final Set<String> STANDARD_EVENT_TYPES;

	static {
		STANDARD_EVENT_TYPES = new LinkedHashSet<>();
		STANDARD_EVENT_TYPES.addAll(VERIFICATION_EVENT_TYPES);
		STANDARD_EVENT_TYPES.addAll(CAEP_EVENT_TYPES);
		STANDARD_EVENT_TYPES.addAll(RISC_EVENT_TYPES);
	}

	@Override
	@PreEnvironment(required = {"ssf"})
	public Environment evaluate(Environment env) {

		JsonElement supportedEventTypesEl = env.getElementFromObject("ssf", "stream.events_supported");
		if (supportedEventTypesEl == null) {
			throw error("Could not find supported event types in stream configuration",
				args("stream_configuration", env.getElementFromObject("ssf", "stream")));
		}

		List<String> supportedEventTypes = OIDFJSON.convertJsonArrayToList(supportedEventTypesEl.getAsJsonArray());

		Set<String> unknownEventTypes = findUnknownEventTypes(supportedEventTypes);

		if (!unknownEventTypes.isEmpty()) {
			throw error("Found unknown event types in stream configuration",
				args("unknown_events", unknownEventTypes, "events_supported", supportedEventTypes, "standard_event_types", STANDARD_EVENT_TYPES));
		} else {
			logSuccess("Only supported event types found in stream configuration",
				args("events_supported", supportedEventTypes));
		}

		return env;
	}

	protected Set<String> findUnknownEventTypes(List<String> supportedEventTypes) {
		Set<String> unknownEventTypes = new LinkedHashSet<>(supportedEventTypes);
		unknownEventTypes.removeAll(STANDARD_EVENT_TYPES);
		return unknownEventTypes;
	}
}
