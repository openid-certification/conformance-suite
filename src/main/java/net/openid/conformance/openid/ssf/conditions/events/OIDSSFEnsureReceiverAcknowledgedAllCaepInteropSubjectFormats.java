package net.openid.conformance.openid.ssf.conditions.events;

import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.openid.ssf.SsfSubjectIdentifiers;
import net.openid.conformance.testmodule.Environment;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Checks that the receiver under test acknowledged at least one CAEP event for every subject
 * identifier format the CAEP Interop Profile §2.5 requires receivers to accept
 * ({@code email} and {@code iss_sub}).
 * <p>
 * Takes the map of generated event {@code jti} → subject format and the set of acknowledged
 * {@code jti} values (HTTP 202 for push delivery, acknowledged via the poll endpoint for poll
 * delivery) collected by the test module.
 */
public class OIDSSFEnsureReceiverAcknowledgedAllCaepInteropSubjectFormats extends AbstractCondition {

	private final Map<String, String> subjectFormatByJti;

	private final Set<String> acknowledgedJtis;

	public OIDSSFEnsureReceiverAcknowledgedAllCaepInteropSubjectFormats(Map<String, String> subjectFormatByJti, Set<String> acknowledgedJtis) {
		this.subjectFormatByJti = Map.copyOf(subjectFormatByJti);
		this.acknowledgedJtis = Set.copyOf(acknowledgedJtis);
	}

	@Override
	public Environment evaluate(Environment env) {

		Set<String> acknowledgedFormats = new TreeSet<>();
		Set<String> unacknowledgedJtis = new LinkedHashSet<>();
		for (Map.Entry<String, String> entry : subjectFormatByJti.entrySet()) {
			if (acknowledgedJtis.contains(entry.getKey())) {
				acknowledgedFormats.add(entry.getValue());
			} else {
				unacknowledgedJtis.add(entry.getKey());
			}
		}

		Set<String> missingFormats = new TreeSet<>(SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS);
		missingFormats.removeAll(acknowledgedFormats);

		if (!missingFormats.isEmpty()) {
			throw error("Receiver did not acknowledge CAEP events for all subject identifier formats required by the CAEP Interop Profile",
				args("required_formats", SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS,
					"acknowledged_formats", acknowledgedFormats,
					"missing_formats", missingFormats,
					"unacknowledged_jtis", unacknowledgedJtis));
		}

		logSuccess("Receiver acknowledged CAEP events for all subject identifier formats required by the CAEP Interop Profile",
			args("acknowledged_formats", acknowledgedFormats, "acknowledged_events", acknowledgedJtis.size()));

		return env;
	}
}
