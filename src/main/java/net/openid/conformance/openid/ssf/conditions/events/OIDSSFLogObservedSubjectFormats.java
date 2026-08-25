package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfEvents;
import net.openid.conformance.openid.ssf.SsfSubjectIdentifiers;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Summarises the subject identifier formats recorded by
 * {@link OIDSSFRecordSecurityEventTokenSubjectFormat} in a single, reviewer-friendly log entry,
 * e.g. {@code email (credential-change, session-revoked), opaque (verification)}, and lists which
 * of the CAEP Interop Profile §2.5 formats the transmitter demonstrated on CAEP events.
 * <p>
 * Fails only if no subject identifier was recorded at all (no SET with a {@code sub_id} was
 * received) — the per-event format checks already deliver the profile verdict.
 */
public class OIDSSFLogObservedSubjectFormats extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonElement observedEl = env.getElementFromObject("ssf", OIDSSFRecordSecurityEventTokenSubjectFormat.OBSERVED_SUBJECT_FORMATS_KEY);
		if (observedEl == null || !observedEl.isJsonObject() || observedEl.getAsJsonObject().isEmpty()) {
			throw error("No subject identifier formats were recorded: no SET with a sub_id claim was received from the transmitter");
		}
		JsonObject observed = observedEl.getAsJsonObject();

		List<String> summary = new ArrayList<>();
		TreeSet<String> caepInteropFormatsOnCaepEvents = new TreeSet<>();
		for (Map.Entry<String, JsonElement> formatEntry : observed.entrySet()) {
			String format = formatEntry.getKey();
			JsonObject details = formatEntry.getValue().getAsJsonObject();

			TreeSet<String> eventNames = new TreeSet<>();
			if (details.has("event_types")) {
				for (String eventType : OIDFJSON.convertJsonArrayToList(details.getAsJsonArray("event_types"))) {
					eventNames.add(eventType.substring(eventType.lastIndexOf('/') + 1));
					if (SsfEvents.CAEP_EVENT_TYPES.contains(eventType) && SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS.contains(format)) {
						caepInteropFormatsOnCaepEvents.add(format);
					}
				}
			}

			String line = format + " (" + String.join(", ", eventNames) + ")";
			if (details.has("members")) {
				List<String> memberDescriptions = new ArrayList<>();
				for (Map.Entry<String, JsonElement> member : details.getAsJsonObject("members").entrySet()) {
					memberDescriptions.add(member.getKey() + "=" + String.join("|", OIDFJSON.convertJsonArrayToList(member.getValue().getAsJsonArray())));
				}
				line += " with members " + String.join(", ", memberDescriptions);
			}
			summary.add(line);
		}

		logSuccess("Subject identifier formats used by the transmitter: " + String.join("; ", summary)
				+ ". CAEP Interop Profile formats demonstrated on CAEP events: "
				+ (caepInteropFormatsOnCaepEvents.isEmpty() ? "none" : String.join(", ", caepInteropFormatsOnCaepEvents)),
			args("observed_subject_formats", observed,
				"caep_interop_formats_on_caep_events", caepInteropFormatsOnCaepEvents,
				"caep_interop_required_formats", SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS));

		return env;
	}
}
