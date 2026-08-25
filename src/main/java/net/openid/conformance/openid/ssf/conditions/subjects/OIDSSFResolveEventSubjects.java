package net.openid.conformance.openid.ssf.conditions.subjects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PostEnvironment;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfSubjectIdentifiers;
import net.openid.conformance.openid.ssf.variant.SsfProfile;
import net.openid.conformance.testmodule.Environment;
import net.openid.conformance.testmodule.OIDFJSON;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Resolves the subject identifiers declared in the test configuration and stores them for the
 * emulated transmitter to use as {@code sub_id} of generated (non-verification) events.
 * <p>
 * The 'SSF valid SubjectId' ({@code ssf.subjects.valid}) field holds the subjects the receiver
 * under test claims to know. It accepts a single RFC 9493 subject identifier object or a JSON
 * list of them. Every entry must be structurally valid per RFC 9493 / SSF 1.0 §3.
 * <p>
 * The result is stored as a JSON array under {@code ssf.event_subjects}.
 * <ul>
 *   <li>{@link SsfProfile#DEFAULT}: all valid subjects are used as configured.</li>
 *   <li>{@link SsfProfile#CAEP_INTEROP}: CAEP Interop Profile §2.5 requires receivers to accept
 *       events with any of the profile's formats ({@code email}, {@code iss_sub}; {@code opaque}
 *       is reserved for the Verification event), so the valid subjects must include at least one
 *       subject per required format. Complex Subjects are used as well; valid subjects in other
 *       formats cannot be used for CAEP events and are skipped with a log entry.</li>
 * </ul>
 */
public class OIDSSFResolveEventSubjects extends AbstractCondition {

	public static final String CONFIG_SECTION = "SSF Transmitter";

	public static final String VALID_SUBJECTS_LABEL = "SSF valid SubjectId";

	@Override
	@PreEnvironment(required = {"config", "ssf"})
	@PostEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		String profile = env.getString("ssf", "profile");

		List<JsonObject> validSubjects = readConfiguredSubjects(env, "ssf.subjects.valid", VALID_SUBJECTS_LABEL);
		if (validSubjects.isEmpty()) {
			throw error("'" + VALID_SUBJECTS_LABEL + "' field is missing from the '" + CONFIG_SECTION + "' section in the test configuration. "
				+ "Configure at least one RFC 9493 subject identifier (or a list of them) that the receiver under test knows.");
		}

		if (SsfProfile.CAEP_INTEROP.name().equals(profile)) {
			validSubjects = restrictToCaepInteropSubjects(validSubjects);
		}

		env.putArray("ssf", "event_subjects", toJsonArray(validSubjects));

		logSuccess("Resolved subject identifiers for generated events", args(
			"profile", profile,
			"event_subjects", toJsonArray(validSubjects)));

		return env;
	}

	/**
	 * Keeps the subjects that may be used for CAEP events under CAEPIOP §2.5 — the simple formats
	 * {@code email} / {@code iss_sub} plus Complex Subjects (SSF 1.0 §3.3, expected to be permitted
	 * by the profile, see openid/sharedsignals#351) — and fails unless every required simple format
	 * is covered by at least one subject. Other formats (e.g. {@code opaque}, which the profile
	 * reserves for the Verification event) are skipped with a log entry.
	 */
	private List<JsonObject> restrictToCaepInteropSubjects(List<JsonObject> validSubjects) {
		List<JsonObject> usable = new ArrayList<>();
		Set<String> coveredFormats = new TreeSet<>();
		for (JsonObject subject : validSubjects) {
			String format = SsfSubjectIdentifiers.getFormat(subject);
			if (SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS.contains(format)) {
				usable.add(subject);
				coveredFormats.add(format);
			} else if (SsfSubjectIdentifiers.FORMAT_COMPLEX.equals(format)) {
				// reported once by OIDSSFWarnCaepInteropComplexSubjectsConfigured
				usable.add(subject);
			} else {
				log("Skipping '" + VALID_SUBJECTS_LABEL + "' entry: its subject identifier format cannot be used for CAEP events under the CAEP Interop Profile (section 2.5)",
					args("subject", subject, "format", format, "permitted_formats", SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS));
			}
		}

		Set<String> missingFormats = new TreeSet<>(SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS);
		missingFormats.removeAll(coveredFormats);
		if (!missingFormats.isEmpty()) {
			throw error("'" + VALID_SUBJECTS_LABEL + "' field in the '" + CONFIG_SECTION + "' section of the test configuration must contain at least one subject identifier "
					+ "for each subject identifier format the CAEP Interop Profile (section 2.5) requires receivers to accept: " + SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS
					+ ". Provide a list such as [{\"format\":\"email\",\"email\":\"user@example.com\"}, {\"format\":\"iss_sub\",\"iss\":\"https://idp.example.com\",\"sub\":\"1234\"}]",
				args("required_formats", SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS,
					"configured_formats", coveredFormats,
					"missing_formats", missingFormats));
		}

		return usable;
	}

	/**
	 * Reads a configuration field holding a single subject identifier object or a list of them.
	 * Returns an empty list when the field is not set; fails on structurally invalid entries.
	 */
	private List<JsonObject> readConfiguredSubjects(Environment env, String configKey, String label) {
		JsonElement configured = env.getElementFromObject("config", configKey);
		List<JsonObject> subjects = new ArrayList<>();
		if (isUnset(configured)) {
			return subjects;
		}

		List<JsonElement> entries = new ArrayList<>();
		if (configured.isJsonArray()) {
			configured.getAsJsonArray().forEach(entries::add);
		} else {
			entries.add(configured);
		}

		Set<String> seen = new LinkedHashSet<>();
		for (int i = 0; i < entries.size(); i++) {
			JsonElement entry = entries.get(i);
			String position = entries.size() > 1 ? " entry " + (i + 1) : "";
			try {
				SsfSubjectIdentifiers.validate(entry);
			} catch (SsfSubjectIdentifiers.InvalidSubjectIdentifierException e) {
				throw error("'" + label + "' field" + position + " in the '" + CONFIG_SECTION + "' section of the test configuration is not a valid RFC 9493 subject identifier: " + e.getMessage(),
					args("config_key", configKey, "subject", entry));
			}
			if (!seen.add(entry.toString())) {
				log("Ignoring duplicate '" + label + "' entry", args("config_key", configKey, "subject", entry));
				continue;
			}
			subjects.add(entry.getAsJsonObject());
		}
		return subjects;
	}

	/**
	 * Treats an absent field, JSON null, an empty object, an empty list and a blank string as
	 * "not configured" so the schedule-test form's empty-field representations are all handled.
	 */
	private static boolean isUnset(JsonElement configured) {
		if (configured == null || configured.isJsonNull()) {
			return true;
		}
		if (configured.isJsonObject()) {
			return configured.getAsJsonObject().isEmpty();
		}
		if (configured.isJsonArray()) {
			return configured.getAsJsonArray().isEmpty();
		}
		if (configured.isJsonPrimitive() && configured.getAsJsonPrimitive().isString()) {
			return OIDFJSON.getString(configured).isBlank();
		}
		return false;
	}

	private static JsonArray toJsonArray(List<JsonObject> subjects) {
		JsonArray array = new JsonArray();
		subjects.forEach(array::add);
		return array;
	}
}
