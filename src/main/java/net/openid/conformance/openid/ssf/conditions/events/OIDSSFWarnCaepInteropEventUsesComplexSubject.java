package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfSubjectIdentifiers;
import net.openid.conformance.testmodule.Environment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Raises a warning when a received CAEP event uses a Complex Subject (SSF 1.0 §3.3) as its
 * {@code sub_id}. Complex Subjects are not rejected: their structure is validated by
 * {@link OIDSSFValidateSecurityEventTokenSubIdClaim} (every member must be a well-formed simple
 * subject identifier, no nesting) and they are only flagged here.
 * <p>
 * The CAEP Interoperability Profile §2.5 currently lists only the simple formats {@code email},
 * {@code iss_sub} and {@code opaque} (Verification event only). However, the CAEP specification's
 * own examples for session-revoked and device-compliance-change use Complex Subjects, and the
 * working group is expected to permit them in the profile
 * (<a href="https://github.com/openid/sharedsignals/issues/351">openid/sharedsignals#351</a>).
 * Callers should therefore run this condition with WARNING severity.
 * <p>
 * To help implementers align with the proposed profile text, the warning reports which Subject
 * Members are present, their formats, whether at least one member uses a profile-supported
 * simple format ({@code email} / {@code iss_sub}), and which member names are not among those
 * defined in SSF 1.0 §3.3 ({@link SsfSubjectIdentifiers#COMPLEX_SUBJECT_MEMBER_NAMES}).
 */
public class OIDSSFWarnCaepInteropEventUsesComplexSubject extends AbstractCondition {

	@Override
	@PreEnvironment(required = {"set_token", "ssf"})
	public Environment evaluate(Environment env) {

		String eventType = env.getString("ssf", "caep_event.type");
		JsonObject claims = env.getElementFromObject("set_token", "claims").getAsJsonObject();
		JsonElement subId = claims.get("sub_id");

		if (!SsfSubjectIdentifiers.isComplex(subId)) {
			logSuccess("CAEP event uses a Simple Subject", args("event_type", eventType, "format", SsfSubjectIdentifiers.getFormat(subId)));
			return env;
		}

		Map<String, String> memberFormats = new LinkedHashMap<>();
		List<String> nonStandardMemberNames = new ArrayList<>();
		boolean hasProfileSupportedMember = false;
		for (Map.Entry<String, JsonElement> member : SsfSubjectIdentifiers.getComplexSubjectMembers(subId).entrySet()) {
			String memberFormat = SsfSubjectIdentifiers.getFormat(member.getValue());
			memberFormats.put(member.getKey(), memberFormat);
			if (memberFormat != null && SsfSubjectIdentifiers.CAEP_INTEROP_EVENT_SUBJECT_FORMATS.contains(memberFormat)) {
				hasProfileSupportedMember = true;
			}
			if (!SsfSubjectIdentifiers.COMPLEX_SUBJECT_MEMBER_NAMES.contains(member.getKey())) {
				nonStandardMemberNames.add(member.getKey());
			}
		}

		String message = "CAEP event uses a Complex Subject. The CAEP Interop Profile section 2.5 currently only lists the simple subject identifier formats "
			+ "email and iss_sub (opaque for the Verification event only); Complex Subjects are expected to be permitted, see openid/sharedsignals#351. "
			+ "The Complex Subject was accepted and its structure validated";
		if (!hasProfileSupportedMember) {
			message += "; note that none of its Subject Members uses a profile-supported format (email or iss_sub), which the proposed profile text would require";
		}
		if (!nonStandardMemberNames.isEmpty()) {
			// SSF 1.0 §3.3 permits additional member names, but a receiver can only interpret the ones
			// it knows — surface them so reviewers can spot typos or vendor-specific members.
			message += "; the Subject Member name(s) " + nonStandardMemberNames + " are not among those defined in SSF 1.0 section 3.3 "
				+ SsfSubjectIdentifiers.COMPLEX_SUBJECT_MEMBER_NAMES.stream().sorted().toList();
		}

		throw error(message, args(
			"event_type", eventType,
			"sub_id", subId,
			"subject_members", memberFormats,
			"has_profile_supported_member", hasProfileSupportedMember,
			"non_standard_member_names", nonStandardMemberNames,
			"discussion", "https://github.com/openid/sharedsignals/issues/351"));
	}
}
