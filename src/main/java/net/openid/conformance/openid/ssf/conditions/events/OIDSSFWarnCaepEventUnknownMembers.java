package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.testmodule.Environment;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Flags members of a CAEP event payload that neither the optional claims common to all events
 * (<a href="https://openid.net/specs/openid-caep-1_0-final.html#section-2">CAEP 1.0 Section 2</a>)
 * nor the event's own definition in Section 3 list. Additional members are permitted (SSF 1.0
 * Section 4.2.3: "Transmitters MAY include additional fields in SSF events"), so callers invoke
 * this at WARNING severity per the suite's unknown-property convention: an unknown member is
 * most often a misspelled optional one, such as {@code event_timestmp}, which every other check
 * would silently treat as absent.
 * <p>
 * Reads {@code ssf.caep_event.type} and {@code ssf.caep_event.data} as stored by
 * {@link OIDSSFExtractCaepEventData}; an event type without a member list here is not checked.
 */
public class OIDSSFWarnCaepEventUnknownMembers extends AbstractCondition {

	static final Set<String> COMMON_MEMBERS = Set.of("event_timestamp", "initiating_entity", "reason_admin", "reason_user");

	static final Map<String, Set<String>> EVENT_SPECIFIC_MEMBERS = Map.of(
		SsfEvents.CAEP_SESSION_REVOKED_EVENT_TYPE, Set.of(),
		SsfEvents.CAEP_TOKEN_CLAIMS_CHANGE_EVENT_TYPE, Set.of("claims"),
		SsfEvents.CAEP_CREDENTIAL_CHANGE_EVENT_TYPE, Set.of("credential_type", "change_type", "friendly_name", "x509_issuer", "x509_serial", "fido2_aaguid"),
		SsfEvents.CAEP_ASSURANCE_LEVEL_CHANGE_EVENT_TYPE, Set.of("namespace", "current_level", "previous_level", "change_direction"),
		SsfEvents.CAEP_DEVICE_COMPLIANCE_CHANGE_EVENT_TYPE, Set.of("previous_status", "current_status"),
		SsfEvents.CAEP_SESSION_ESTABLISHED_EVENT_TYPE, Set.of("fp_ua", "acr", "amr", "ext_id"),
		SsfEvents.CAEP_SESSION_PRESENTED_EVENT_TYPE, Set.of("fp_ua", "ext_id"),
		SsfEvents.CAEP_RISK_LEVEL_CHANGE_EVENT_TYPE, Set.of("risk_reason", "principal", "current_level", "previous_level"));

	@PreEnvironment(required = {"ssf"})
	@Override
	public Environment evaluate(Environment env) {

		String eventType = env.getString("ssf", "caep_event.type");
		JsonObject eventData = env.getElementFromObject("ssf", "caep_event.data").getAsJsonObject();

		Set<String> eventSpecificMembers = EVENT_SPECIFIC_MEMBERS.get(eventType);
		if (eventSpecificMembers == null) {
			log("No member list for this event type, nothing to check", args("event_type", eventType));
			return env;
		}

		Set<String> knownMembers = new TreeSet<>(COMMON_MEMBERS);
		knownMembers.addAll(eventSpecificMembers);

		Set<String> unknownMembers = new TreeSet<>(eventData.keySet());
		unknownMembers.removeAll(knownMembers);
		if (!unknownMembers.isEmpty()) {
			throw error("The event contains members that neither the common CAEP event claims nor the event definition define. "
					+ "This may indicate a misspelled member name.",
				args("event_type", eventType, "event_data", eventData, "unknown_members", unknownMembers, "known_members", knownMembers));
		}

		logSuccess("The event contains only members the common CAEP event claims and the event definition define",
			args("event_type", eventType, "event_data", eventData));

		return env;
	}
}
