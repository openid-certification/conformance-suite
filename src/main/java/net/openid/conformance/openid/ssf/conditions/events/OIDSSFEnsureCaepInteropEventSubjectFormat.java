package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfSubjectIdentifiers;
import net.openid.conformance.testmodule.Environment;

/**
 * Checks that the {@code sub_id} of a received CAEP event uses one of the subject identifier
 * formats a transmitter may send under the CAEP Interoperability Profile §2.5:
 * {@code email} or {@code iss_sub}. The {@code opaque} format is permitted for the Verification
 * event only and is therefore rejected on CAEP events.
 * <p>
 * Complex Subjects (SSF 1.0 §3.3) are passed through here and reported separately by
 * {@link OIDSSFWarnCaepInteropEventUsesComplexSubject}, so the caller can assign a different
 * severity to them while the working group settles their status
 * (<a href="https://github.com/openid/sharedsignals/issues/351">openid/sharedsignals#351</a>).
 * <p>
 * Expects {@code set_token.claims.sub_id} (see {@link OIDSSFParseSecurityEventToken}) and
 * {@code ssf.caep_event.type} (see {@link OIDSSFExtractCaepEventData}).
 */
public class OIDSSFEnsureCaepInteropEventSubjectFormat extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"set_token", "ssf"})
	public Environment evaluate(Environment env) {

		String eventType = env.getString("ssf", "caep_event.type");
		JsonObject claims = env.getElementFromObject("set_token", "claims").getAsJsonObject();
		JsonElement subId = claims.get("sub_id");

		String format = SsfSubjectIdentifiers.getFormat(subId);
		if (format == null) {
			throw error("CAEP event 'sub_id' claim is missing or has no 'format' member",
				args("event_type", eventType, "sub_id", subId));
		}

		if (SsfSubjectIdentifiers.isComplex(subId)) {
			log("CAEP event uses a Complex Subject; see the separate Complex Subject check for the profile verdict",
				args("event_type", eventType, "sub_id", subId));
			return env;
		}

		if (SsfSubjectIdentifiers.FORMAT_OPAQUE.equals(format)) {
			throw error("CAEP event uses the 'opaque' subject identifier format, which the CAEP Interop Profile only permits for the Verification event. "
					+ "CAEP events must use one of: " + SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS,
				args("event_type", eventType, "sub_id", subId, "format", format,
					"permitted_formats", SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS));
		}

		if (!SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS.contains(format)) {
			throw error("CAEP event uses a subject identifier format that is not supported by the CAEP Interop Profile. "
					+ "CAEP events must use one of: " + SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS,
				args("event_type", eventType, "sub_id", subId, "format", format,
					"permitted_formats", SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS));
		}

		logSuccess("CAEP event uses a subject identifier format supported by the CAEP Interop Profile",
			args("event_type", eventType, "format", format, "sub_id", subId));

		return env;
	}
}
