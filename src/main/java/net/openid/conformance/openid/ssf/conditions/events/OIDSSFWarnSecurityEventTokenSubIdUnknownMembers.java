package net.openid.conformance.openid.ssf.conditions.events;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.openid.conformance.condition.AbstractCondition;
import net.openid.conformance.condition.PreEnvironment;
import net.openid.conformance.openid.ssf.SsfSubjectIdentifiers;
import net.openid.conformance.testmodule.Environment;

import java.util.List;

/**
 * Flags members of the {@code sub_id} claim that RFC 9493 §3 does not describe for the used
 * subject identifier format — "A Subject Identifier MUST NOT contain any members prohibited or
 * not described by its Identifier Format". These often indicate a misspelled member or members
 * copied from another format (e.g. an {@code iss} on an {@code email} subject).
 * <p>
 * Callers should invoke this at WARNING severity per the suite's unknown-property convention;
 * Complex Subject member names and proprietary formats are not restricted and are not flagged
 * (only the members nested inside them are). Structural validity is checked separately by
 * {@link OIDSSFValidateSecurityEventTokenSubIdClaim}.
 */
public class OIDSSFWarnSecurityEventTokenSubIdUnknownMembers extends AbstractCondition {

	@Override
	@PreEnvironment(required = "set_token")
	public Environment evaluate(Environment env) {

		JsonObject claims = env.getElementFromObject("set_token", "claims").getAsJsonObject();
		JsonElement subId = claims.get("sub_id");
		if (subId == null) {
			log("SET has no sub_id claim, nothing to check", args("claims", claims));
			return env;
		}

		List<String> unknownMembers = SsfSubjectIdentifiers.findUnknownMembers(subId);
		if (!unknownMembers.isEmpty()) {
			throw error("The 'sub_id' claim contains members that RFC 9493 (section 3) does not describe for its subject identifier format "
					+ "(\"A Subject Identifier MUST NOT contain any members prohibited or not described by its Identifier Format\"). "
					+ "This may indicate a misspelled member name.",
				args("sub_id", subId, "format", SsfSubjectIdentifiers.getFormat(subId), "unknown_members", unknownMembers));
		}

		logSuccess("The 'sub_id' claim contains only members described for its subject identifier format",
			args("sub_id", subId, "format", SsfSubjectIdentifiers.getFormat(subId)));

		return env;
	}
}
