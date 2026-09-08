package net.openid.conformance.openid.ssf.conditions.subjects;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfSubjectIdentifiers;
import net.openid.conformance.testmodule.Environment;

/**
 * Raises a single configuration-time warning when the 'SSF valid SubjectId' configuration (as
 * resolved by {@link OIDSSFResolveEventSubjects} into {@code ssf.event_subjects}) contains Complex
 * Subjects (SSF 1.0 §3.3). Events are generated for them like for any other declared subject, but
 * they are not listed in CAEP Interop Profile §2.5 yet; the working group is expected to permit
 * them (<a href="https://github.com/openid/sharedsignals/issues/351">openid/sharedsignals#351</a>).
 * <p>
 * Callers run this with WARNING severity; once the profile permits Complex Subjects, lower the
 * severity at the call site to INFO so this becomes a plain log message.
 */
public class OIDSSFWarnCaepInteropComplexSubjectsConfigured extends AbstractCondition {

	@Override
	@PreEnvironment(required = "ssf")
	public Environment evaluate(Environment env) {

		JsonArray complexSubjects = new JsonArray();
		JsonElement subjects = env.getElementFromObject("ssf", "event_subjects");
		if (subjects != null && subjects.isJsonArray()) {
			for (JsonElement subject : subjects.getAsJsonArray()) {
				if (SsfSubjectIdentifiers.isComplex(subject)) {
					complexSubjects.add(subject);
				}
			}
		}

		if (complexSubjects.isEmpty()) {
			logSuccess("No Complex Subjects configured; all configured subjects are Simple Subjects");
			return env;
		}

		throw error("The '" + OIDSSFResolveEventSubjects.VALID_SUBJECTS_LABEL + "' test configuration declares " + complexSubjects.size() + " Complex Subject(s). "
				+ "Events will be generated for them, but Complex Subjects are not yet listed in CAEP Interop Profile section 2.5 "
				+ "(email, iss_sub; opaque for the Verification event only); they are expected to be permitted, see openid/sharedsignals#351",
			args("complex_subjects", complexSubjects,
				"discussion", "https://github.com/openid/sharedsignals/issues/351"));
	}
}
